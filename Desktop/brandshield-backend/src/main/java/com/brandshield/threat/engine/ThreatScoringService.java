package com.brandshield.threat.engine;

import com.brandshield.brand.entity.Brand;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.ThreatType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ThreatScoringService {

    private final TyposquattingDetector typosquattingDetector;

    private static final List<String> PHISHING_KEYWORDS = List.of(
            "login", "signin", "verify", "secure", "auth", "account", "wallet", "password", "update", "billing"
    );

    private static final List<String> COUNTERFEIT_KEYWORDS = List.of(
            "cheap", "replica", "fake", "discount", "outlet", "wholesale", "sale", "free", "copy"
    );

    private static final List<String> HIGH_RISK_TLDS = List.of(
            ".xyz", ".top", ".cc", ".ru", ".cn", ".tk", ".ml", ".ga", ".cf", ".gq"
    );

    public ThreatScoringService(TyposquattingDetector typosquattingDetector) {
        this.typosquattingDetector = typosquattingDetector;
    }

    public record AnalysisResult(
            int score,
            Severity severity,
            ThreatType threatType,
            String explanation
    ) {}

    public AnalysisResult analyzeTarget(String target, Brand brand) {
        String lowerTarget = target.toLowerCase().trim();

        // Check if domain is in official whitelist
        if (brand.getOfficialDomains().contains(lowerTarget) ||
            brand.getOfficialWebsite().toLowerCase().contains(lowerTarget)) {
            return new AnalysisResult(0, Severity.LOW, ThreatType.SUSPICIOUS_DOMAIN, "Target matches official whitelist");
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // 1. Keyword & Brand Name checks
        String brandName = brand.getName().toLowerCase();
        boolean containsBrandName = lowerTarget.contains(brandName);
        if (containsBrandName) {
            score += 35;
            reasons.append("Contains brand name '").append(brandName).append("'. ");
        }

        // 2. Typosquatting Similarity check against official domains
        double maxSimilarity = 0.0;
        String closestDomain = "";
        for (String officialDomain : brand.getOfficialDomains()) {
            double sim = typosquattingDetector.calculateSimilarity(lowerTarget, officialDomain.toLowerCase());
            if (sim > maxSimilarity) {
                maxSimilarity = sim;
                closestDomain = officialDomain;
            }
        }

        if (maxSimilarity >= 0.85 && maxSimilarity < 1.0) {
            score += 40;
            reasons.append("High lookalike similarity (")
                    .append((int) (maxSimilarity * 100))
                    .append("%) to official domain '")
                    .append(closestDomain)
                    .append("'. ");
        } else if (maxSimilarity >= 0.70) {
            score += 25;
            reasons.append("Moderate similarity to '").append(closestDomain).append("'. ");
        }

        // 3. Phishing Indicators
        boolean isPhishing = false;
        int phishingMatches = 0;
        for (String pKey : PHISHING_KEYWORDS) {
            if (lowerTarget.contains(pKey)) {
                phishingMatches++;
                reasons.append("Detected high-risk phishing indicator: '").append(pKey).append("'. ");
            }
        }
        if (phishingMatches > 0) {
            isPhishing = true;
            score += Math.min(40, 20 + (phishingMatches - 1) * 10);
        }

        // 4. Counterfeit Indicators
        boolean isCounterfeit = false;
        for (String cKey : COUNTERFEIT_KEYWORDS) {
            if (lowerTarget.contains(cKey)) {
                score += 20;
                isCounterfeit = true;
                reasons.append("Detected counterfeit/scam keyword: '").append(cKey).append("'. ");
                break;
            }
        }

        // 5. High-Risk TLD check
        for (String tld : HIGH_RISK_TLDS) {
            if (lowerTarget.endsWith(tld) || lowerTarget.contains(tld + "/") || lowerTarget.contains(tld + "?")) {
                score += 15;
                reasons.append("Registered under high-risk TLD '").append(tld).append("'. ");
                break;
            }
        }

        // 6. Check additional brand keywords
        Set<String> brandKeywords = brand.getKeywords();
        for (String kw : brandKeywords) {
            if (!kw.equalsIgnoreCase(brandName) && lowerTarget.contains(kw.toLowerCase())) {
                score += 15;
                reasons.append("Matches protected brand keyword '").append(kw).append("'. ");
                break;
            }
        }

        // Cap score between 0 and 100
        score = Math.min(100, Math.max(0, score));

        // Determine ThreatType
        ThreatType threatType;
        if (isPhishing) {
            threatType = ThreatType.PHISHING;
        } else if (maxSimilarity >= 0.75) {
            threatType = ThreatType.TYPOSQUATTING;
        } else if (isCounterfeit) {
            threatType = ThreatType.COUNTERFEIT;
        } else if (containsBrandName) {
            threatType = ThreatType.IMPERSONATION;
        } else {
            threatType = ThreatType.SUSPICIOUS_DOMAIN;
        }

        // Determine Severity
        Severity severity;
        if (score >= 80) {
            severity = Severity.CRITICAL;
        } else if (score >= 60) {
            severity = Severity.HIGH;
        } else if (score >= 40) {
            severity = Severity.MEDIUM;
        } else {
            severity = Severity.LOW;
        }

        return new AnalysisResult(score, severity, threatType, reasons.toString().trim());
    }
}
