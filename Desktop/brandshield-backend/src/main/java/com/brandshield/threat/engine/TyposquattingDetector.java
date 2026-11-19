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

    // Expanded popular and high-risk TLDs including country-codes like .co.in, .in, .co
    public static final List<String> POPULAR_AND_RISKY_TLDS = List.of(
            ".co.in", ".in", ".com", ".net", ".org", ".co",
            ".xyz", ".top", ".shop", ".online", ".site", ".store",
            ".live", ".info", ".biz", ".club", ".app", ".cc"
    );

    private static final List<String> SUSPICIOUS_AFFIXES = List.of(
            "login", "verify", "secure", "support", "official", "store", "shop", "discount", "cheap", "account", "booking"
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
     * Generates a comprehensive set of likely typosquatting, word-reversal, and lookalike domain variations.
     */
    public Set<String> generateVariations(String domainName) {
        return generateVariations(domainName, Set.of());
    }

    /**
     * Generates domain variations with brand keyword awareness (handles compound words & transpositions).
     */
    public Set<String> generateVariations(String domainName, Set<String> brandKeywords) {
        Set<String> variations = new HashSet<>();
        if (domainName == null || domainName.isBlank()) return variations;

        String base = domainName.toLowerCase().trim()
                .replaceFirst("^(https?://)?(www\\.)?", "")
                .split("/")[0];

        String namePart = base;
        String tldPart = ".com";

        int firstDot = base.indexOf('.');
        if (firstDot > 0) {
            namePart = base.substring(0, firstDot);
            tldPart = base.substring(firstDot);
        }

        // 1. Omission of characters
        for (int i = 0; i < namePart.length(); i++) {
            String variant = namePart.substring(0, i) + namePart.substring(i + 1);
            if (variant.length() >= 3) {
                variations.add(variant + tldPart);
                variations.add(variant + ".in");
                variations.add(variant + ".co.in");
            }
        }

        // 2. Duplication of characters (e.g. hotel -> hotell)
        for (int i = 0; i < namePart.length(); i++) {
            String variant = namePart.substring(0, i) + namePart.charAt(i) + namePart.substring(i);
            variations.add(variant + tldPart);
            variations.add(variant + ".co.in");
            variations.add(variant + ".in");
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

        // 6. Common & Regional TLD variations (.co.in, .in, .net, .org, .xyz, etc.)
        for (String tld : POPULAR_AND_RISKY_TLDS) {
            if (!tld.equalsIgnoreCase(tldPart)) {
                variations.add(namePart + tld);
            }
        }

        // 7. Compound Word Reversals & Transpositions (e.g. hotelthreeleaves -> threeleaveshotel, threeleaveshotell)
        List<String> keywords = new ArrayList<>();
        if (brandKeywords != null) {
            for (String kw : brandKeywords) {
                // Split multi-word keywords on space, hyphen, underscore
                for (String token : kw.trim().toLowerCase().split("[\\s\\-_]+")) {
                    String clean = token.replaceAll("[^a-z0-9]", "");
                    if (clean.length() >= 3 && !keywords.contains(clean)) {
                        keywords.add(clean);
                    }
                }
            }
        }

        // Also extract compound tokens directly from the domain name part (e.g. hotelthreeleaves -> hotel, threeleaves)
        List<String> domainCompoundTokens = extractCompoundTokens(namePart);
        if (!domainCompoundTokens.isEmpty() && keywords.size() < 2) {
            keywords = domainCompoundTokens;
        }

        // If multiple keywords exist, generate permutations across TLDs
        if (keywords.size() >= 2) {
            String combinedReversed = String.join("", reversedList(keywords));
            String combinedOriginal = String.join("", keywords);

            for (String tld : POPULAR_AND_RISKY_TLDS) {
                variations.add(combinedReversed + tld);
                variations.add(combinedOriginal + tld);
                variations.add(String.join("-", reversedList(keywords)) + tld);
                // Extra letter at end: e.g. threeleaveshotell.co.in
                variations.add(combinedReversed + combinedReversed.charAt(combinedReversed.length() - 1) + tld);
                variations.add(combinedOriginal + combinedOriginal.charAt(combinedOriginal.length() - 1) + tld);
            }
        }

        // If compound tokens found from domain, generate specific permutations with character duplication
        if (!domainCompoundTokens.isEmpty()) {
            String p1 = domainCompoundTokens.get(0);
            String p2 = domainCompoundTokens.get(1);
            for (String tld : POPULAR_AND_RISKY_TLDS) {
                // Reversed: p2 + p1 (e.g. threeleaveshotel)
                variations.add(p2 + p1 + tld);
                variations.add(p2 + "-" + p1 + tld);
                // Duplicate last char of p1: e.g. threeleaveshotell
                variations.add(p2 + p1 + p1.charAt(p1.length() - 1) + tld);
                // Duplicate last char of p2: e.g. threeleavesshotel
                variations.add(p2 + p2.charAt(p2.length() - 1) + p1 + tld);
            }
        }

        // Remove the original domain if generated
        variations.remove(base);
        return variations;
    }

    private static final List<String> COMMON_COMPOUND_PREFIXES = List.of(
            "hotel", "motel", "resort", "inn", "stay", "villa", "club",
            "shop", "store", "pay", "bank", "app", "tech", "group",
            "care", "star", "grand", "royal", "online", "portal", "official", "live", "global", "prime", "direct", "booking"
    );

    private List<String> extractCompoundTokens(String namePart) {
        if (namePart == null || namePart.length() < 6) return List.of();
        for (String prefix : COMMON_COMPOUND_PREFIXES) {
            if (namePart.startsWith(prefix) && namePart.length() > prefix.length() + 2) {
                return List.of(prefix, namePart.substring(prefix.length()));
            }
            if (namePart.endsWith(prefix) && namePart.length() > prefix.length() + 2) {
                return List.of(namePart.substring(0, namePart.length() - prefix.length()), prefix);
            }
        }
        return List.of();
    }

    private List<String> reversedList(List<String> list) {
        List<String> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }
}
