package com.brandshield.threat.engine;

import com.brandshield.brand.entity.Brand;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.ThreatType;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ThreatScoringService {

    private final TyposquattingDetector typosquattingDetector;

    private static final List<String> PHISHING_KEYWORDS = List.of(
            "login", "signin", "verify", "secure", "auth", "account", "wallet", "password", "update", "billing", "booking", "pay"
    );

    private static final List<String> COUNTERFEIT_KEYWORDS = List.of(
            "cheap", "replica", "fake", "discount", "outlet", "wholesale", "sale", "free", "copy"
    );

    private static final List<String> HIGH_RISK_AND_POPULAR_TLDS = List.of(
            ".xyz", ".top", ".cc", ".ru", ".cn", ".tk", ".ml", ".ga", ".cf", ".gq",
            ".co.in", ".in", ".online", ".site", ".store", ".live", ".info", ".biz", ".vip"
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
        String cleanTarget = target.toLowerCase().trim()
                .replaceFirst("^(https?://)?(www\\.)?", "")
                .split("/")[0]
                .split(":")[0];

        // 1. Check if domain is in official whitelist
        if (brand.getOfficialDomains() != null && brand.getOfficialDomains().contains(cleanTarget)) {
            return new AnalysisResult(0, Severity.LOW, ThreatType.SUSPICIOUS_DOMAIN, "Target matches official whitelist");
        }
        if (brand.getOfficialWebsite() != null) {
            String cleanOfficialSite = brand.getOfficialWebsite().toLowerCase().trim()
                    .replaceFirst("^(https?://)?(www\\.)?", "").split("/")[0];
            if (cleanOfficialSite.equalsIgnoreCase(cleanTarget)) {
                return new AnalysisResult(0, Severity.LOW, ThreatType.SUSPICIOUS_DOMAIN, "Target matches official website");
            }
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // Extract target stem without TLD (e.g. threeleaveshotell.co.in -> threeleaveshotell)
        String targetStem = cleanTarget;
        int firstDot = cleanTarget.indexOf('.');
        if (firstDot > 0) {
            targetStem = cleanTarget.substring(0, firstDot);
        }

        // 2. Keyword & Brand Token Extraction
        Set<String> brandTokens = new HashSet<>();
        String brandNoSpaces = "";
        if (brand.getName() != null) {
            String nameClean = brand.getName().toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
            for (String part : nameClean.split("\\s+")) {
                if (part.length() >= 3) brandTokens.add(part);
            }
            brandNoSpaces = brand.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        if (brand.getKeywords() != null) {
            for (String kw : brand.getKeywords()) {
                String kwClean = kw.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
                for (String part : kwClean.split("\\s+")) {
                    if (part.length() >= 3) brandTokens.add(part);
                }
            }
        }

        // Add tokens from official domains (e.g. hotelthreeleaves.com -> hotel, three, leaves)
        for (String offDomain : brand.getOfficialDomains()) {
            String offStem = offDomain.toLowerCase().replaceFirst("^(https?://)?(www\\.)?", "").split("\\.")[0];
            if (offStem.length() >= 3) {
                // Split camelCase or compound if needed
                brandTokens.add(offStem);
            }
        }

        // 3. Evaluate Brand Token Presence & Typosquatting in Target
        int matchedTokens = 0;
        int typoTokens = 0;
        List<String> matchedTokenNames = new ArrayList<>();

        for (String token : brandTokens) {
            if (token.equalsIgnoreCase(brandNoSpaces)) continue;

            if (targetStem.contains(token)) {
                matchedTokens++;
                matchedTokenNames.add(token);

                // Detect character duplication at end of token (e.g. "hotel" -> "hotell")
                int idx = targetStem.indexOf(token);
                if (idx + token.length() < targetStem.length()) {
                    char lastChar = token.charAt(token.length() - 1);
                    char nextChar = targetStem.charAt(idx + token.length());
                    if (lastChar == nextChar) {
                        typoTokens++;
                        reasons.append("Character duplication detected on token '").append(token).append("'. ");
                    }
                }
            } else if (token.length() >= 4) {
                // Check if target contains a 1-character typo of this token
                for (int i = 0; i <= targetStem.length() - token.length(); i++) {
                    String sub = targetStem.substring(i, Math.min(targetStem.length(), i + token.length() + 1));
                    int dist = typosquattingDetector.computeLevenshteinDistance(sub, token);
                    if (dist == 1) {
                        typoTokens++;
                        reasons.append("Contains typo variant of '").append(token).append("' ('").append(sub).append("'). ");
                        break;
                    }
                }
            }
        }

        // Direct brand name match vs Multi-keyword hijack
        if (!brandNoSpaces.isBlank() && targetStem.contains(brandNoSpaces)) {
            score += 50;
            reasons.append("Exact brand name detected in target domain. ");
        } else if (matchedTokens >= 3 || (matchedTokens >= 2 && matchedTokens >= brandTokens.size() - 1)) {
            score += 60;
            reasons.append("High-confidence brand lookalike: matched core brand terms [")
                    .append(String.join(", ", matchedTokenNames))
                    .append("] in compound permutation. ");
        } else if (matchedTokens >= 2) {
            score += 45;
            reasons.append("Compound brand lookalike: matched keywords [")
                    .append(String.join(", ", matchedTokenNames))
                    .append("]. ");
        } else if (matchedTokens >= 1) {
            score += 25;
            reasons.append("Matches protected brand keyword '").append(matchedTokenNames.get(0)).append("'. ");
        }

        if (typoTokens > 0) {
            score += 20; // Extra penalty for typosquatting / character tampering
        }

        // 4. Similarity check against official domains
        double maxSimilarity = 0.0;
        String closestDomain = "";
        for (String officialDomain : brand.getOfficialDomains()) {
            double sim = typosquattingDetector.calculateSimilarity(cleanTarget, officialDomain.toLowerCase());
            if (sim > maxSimilarity) {
                maxSimilarity = sim;
                closestDomain = officialDomain;
            }
        }

        if (maxSimilarity >= 0.80 && maxSimilarity < 1.0) {
            score += 35;
            reasons.append("High lookalike similarity (")
                    .append((int) (maxSimilarity * 100))
                    .append("%) to official domain '")
                    .append(closestDomain)
                    .append("'. ");
        } else if (maxSimilarity >= 0.65) {
            score += 20;
            reasons.append("Moderate similarity to '").append(closestDomain).append("'. ");
        }

        // 5. Phishing Indicators
        boolean isPhishing = false;
        int phishingMatches = 0;
        for (String pKey : PHISHING_KEYWORDS) {
            if (cleanTarget.contains(pKey)) {
                phishingMatches++;
                reasons.append("Detected credential/phishing keyword: '").append(pKey).append("'. ");
            }
        }
        if (phishingMatches > 0) {
            isPhishing = true;
            score += Math.min(40, 20 + (phishingMatches - 1) * 10);
        }

        // 6. Counterfeit Indicators
        boolean isCounterfeit = false;
        for (String cKey : COUNTERFEIT_KEYWORDS) {
            if (cleanTarget.contains(cKey)) {
                score += 20;
                isCounterfeit = true;
                reasons.append("Detected counterfeit/scam keyword: '").append(cKey).append("'. ");
                break;
            }
        }

        // 7. High-Risk & Alternate TLD check
        for (String tld : HIGH_RISK_AND_POPULAR_TLDS) {
            if (cleanTarget.endsWith(tld) || cleanTarget.contains(tld + "/") || cleanTarget.contains(tld + "?")) {
                score += 15;
                reasons.append("Registered under alternate/suspicious TLD '").append(tld).append("'. ");
                break;
            }
        }

        // Cap score between 0 and 100
        score = Math.min(100, Math.max(0, score));

        // Determine ThreatType
        ThreatType threatType;
        if (isPhishing) {
            threatType = ThreatType.PHISHING;
        } else if (typoTokens > 0 || maxSimilarity >= 0.70) {
            threatType = ThreatType.TYPOSQUATTING;
        } else if (isCounterfeit) {
            threatType = ThreatType.COUNTERFEIT;
        } else if (matchedTokens >= 2 || (!brandNoSpaces.isBlank() && targetStem.contains(brandNoSpaces))) {
            threatType = ThreatType.IMPERSONATION;
        } else {
            threatType = ThreatType.SUSPICIOUS_DOMAIN;
        }

        // Determine Severity
        Severity severity;
        if (score >= 75) {
            severity = Severity.CRITICAL;
        } else if (score >= 50) {
            severity = Severity.HIGH;
        } else if (score >= 30) {
            severity = Severity.MEDIUM;
        } else {
            severity = Severity.LOW;
        }

        return new AnalysisResult(score, severity, threatType, reasons.toString().trim());
    }
}
