package com.brandshield.intelligence;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.util.*;

@Service
public class WebsiteContentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteContentAnalysisService.class);

    public record WebsiteAnalysisResult(
            boolean isLive,
            int statusCode,
            String title,
            String description,
            List<String> headings,
            List<String> imageSources,
            boolean hasPasswordField,
            boolean hasLoginForm,
            double textSimilarityScore,
            String detectedBrandMention
    ) {}

    /**
     * Inspects target URL safely via Jsoup and calculates similarity with official website text.
     */
    public WebsiteAnalysisResult analyzeWebsite(String targetUrl, String officialUrl, String brandName) {
        String safeTarget = sanitizeUrl(targetUrl);
        if (!isSafePublicUrl(safeTarget)) {
            return new WebsiteAnalysisResult(false, 0, "", "", List.of(), List.of(), false, false, 0.0, "Blocked / Unsafe URL");
        }

        try {
            Document targetDoc = Jsoup.connect(safeTarget)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) BrandShield-Scanner/1.0")
                    .timeout(5000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            String title = targetDoc.title();
            String description = "";
            Element metaDesc = targetDoc.selectFirst("meta[name=description], meta[property=og:description]");
            if (metaDesc != null) {
                description = metaDesc.attr("content");
            }

            // Extract headings
            List<String> headings = new ArrayList<>();
            for (Element h : targetDoc.select("h1, h2")) {
                String text = h.text().trim();
                if (!text.isBlank() && headings.size() < 5) {
                    headings.add(text);
                }
            }

            // Extract images and logos
            List<String> images = new ArrayList<>();
            for (Element img : targetDoc.select("img[src]")) {
                String src = img.attr("abs:src");
                if (!src.isBlank() && images.size() < 6) {
                    images.add(src);
                }
            }

            // Check for credential forms (phishing indicators)
            boolean hasPassword = !targetDoc.select("input[type=password]").isEmpty();
            boolean hasLoginForm = hasPassword || !targetDoc.select("form[action*='login'], form[action*='signin']").isEmpty();

            // Brand mention check
            String pageText = targetDoc.body() != null ? targetDoc.body().text().toLowerCase() : "";
            boolean mentionsBrand = brandName != null && pageText.contains(brandName.toLowerCase());

            // Compute similarity with official site if available
            double similarity = 0.0;
            if (officialUrl != null && !officialUrl.isBlank()) {
                similarity = computeJaccardSimilarity(pageText, fetchSampleText(officialUrl));
            }

            return new WebsiteAnalysisResult(
                    true,
                    200,
                    title,
                    description,
                    headings,
                    images,
                    hasPassword,
                    hasLoginForm,
                    Math.round(similarity * 100.0) / 100.0,
                    mentionsBrand ? "Direct Brand Mention in Page Body" : "No Explicit Mention"
            );

        } catch (Exception e) {
            log.debug("Jsoup website content inspection failed for {}: {}", safeTarget, e.getMessage());
            return new WebsiteAnalysisResult(false, 0, "Unreachable", "", List.of(), List.of(), false, false, 0.0, "Site Offline or Connection Timed Out");
        }
    }

    private String fetchSampleText(String url) {
        try {
            String safe = sanitizeUrl(url);
            if (!isSafePublicUrl(safe)) return "";
            Document doc = Jsoup.connect(safe).timeout(3000).get();
            return doc.body() != null ? doc.body().text().toLowerCase() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private double computeJaccardSimilarity(String text1, String text2) {
        if (text1.isBlank() || text2.isBlank()) return 0.0;
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\W+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\W+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * SSRF Protection: Ensures destination IP is not private, loopback, or cloud-metadata address.
     */
    private boolean isSafePublicUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            InetAddress address = InetAddress.getByName(host);
            return !address.isLoopbackAddress()
                    && !address.isSiteLocalAddress()
                    && !address.isLinkLocalAddress()
                    && !address.isAnyLocalAddress()
                    && !host.equalsIgnoreCase("169.254.169.254"); // AWS/Cloud metadata
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizeUrl(String url) {
        String clean = url.trim();
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        return clean;
    }
}
