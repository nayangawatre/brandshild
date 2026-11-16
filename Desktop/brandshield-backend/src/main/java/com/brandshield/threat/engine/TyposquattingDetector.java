package com.brandshield.threat.engine;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TyposquattingDetector {

    private static final Map<Character, List<Character>> HOMOGLYPHS = Map.of(
            'o', List.of('0'),
            'i', List.of('1', 'l'),
            'l', List.of('1', 'i'),
            'e', List.of('3'),
            'a', List.of('4', '@'),
            's', List.of('5', '$'),
            't', List.of('7')
    );

    private static final List<String> COMMON_TLDS = List.of(
            ".xyz", ".top", ".shop", ".online", ".site", ".store", ".cc", ".biz", ".info"
    );

    private static final List<String> SUSPICIOUS_AFFIXES = List.of(
            "login", "verify", "secure", "support", "official", "store", "shop", "discount", "cheap", "account"
    );

    /**
     * Computes the Levenshtein edit distance between two strings.
     */
    public int computeLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculates similarity percentage between 0.0 and 1.0.
     */
    public double calculateSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Generates a curated set of likely typosquatting/lookalike domain variations.
     */
    public Set<String> generateVariations(String domainName) {
        Set<String> variations = new HashSet<>();
        if (domainName == null || domainName.isBlank()) return variations;

        String base = domainName.toLowerCase().trim();
        String namePart = base;
        String tldPart = ".com";

        int lastDot = base.lastIndexOf('.');
        if (lastDot > 0) {
            namePart = base.substring(0, lastDot);
            tldPart = base.substring(lastDot);
        }

        // 1. Omission of characters
        for (int i = 0; i < namePart.length(); i++) {
            String variant = namePart.substring(0, i) + namePart.substring(i + 1);
            if (variant.length() >= 3) {
                variations.add(variant + tldPart);
            }
        }

        // 2. Duplication of characters
        for (int i = 0; i < namePart.length(); i++) {
            String variant = namePart.substring(0, i) + namePart.charAt(i) + namePart.substring(i);
            variations.add(variant + tldPart);
        }

        // 3. Adjacent transposition
        for (int i = 0; i < namePart.length() - 1; i++) {
            char[] chars = namePart.toCharArray();
            char temp = chars[i];
            chars[i] = chars[i + 1];
            chars[i + 1] = temp;
            variations.add(new String(chars) + tldPart);
        }

        // 4. Homoglyph character substitutions
        for (int i = 0; i < namePart.length(); i++) {
            char c = namePart.charAt(i);
            if (HOMOGLYPHS.containsKey(c)) {
                for (char sub : HOMOGLYPHS.get(c)) {
                    String variant = namePart.substring(0, i) + sub + namePart.substring(i + 1);
                    variations.add(variant + tldPart);
                }
            }
        }

        // 5. Affix combinations
        for (String affix : SUSPICIOUS_AFFIXES) {
            variations.add(namePart + "-" + affix + tldPart);
            variations.add(affix + "-" + namePart + tldPart);
            variations.add(namePart + affix + tldPart);
        }

        // 6. Suspicious TLD variations
        for (String tld : COMMON_TLDS) {
            if (!tld.equalsIgnoreCase(tldPart)) {
                variations.add(namePart + tld);
            }
        }

        // Remove the original domain if generated
        variations.remove(base);
        return variations;
    }
}
