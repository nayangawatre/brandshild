package com.brandshield.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

@Service
public class VisualSimilarityService {

    private static final Logger log = LoggerFactory.getLogger(VisualSimilarityService.class);

    public record VisualComparisonResult(
            double similarityScore,
            String assessment,
            boolean isSuspectedClone
    ) {}

    /**
     * Compares two image URLs (e.g. logos/favicons) using perceptual difference hashing.
     */
    public VisualComparisonResult compareImages(String targetImageUrl, String officialImageUrl) {
        if (targetImageUrl == null || officialImageUrl == null || targetImageUrl.isBlank() || officialImageUrl.isBlank()) {
            return new VisualComparisonResult(0.0, "Insufficient image data for visual comparison", false);
        }

        try {
            BufferedImage img1 = fetchAndNormalize(targetImageUrl);
            BufferedImage img2 = fetchAndNormalize(officialImageUrl);

            if (img1 == null || img2 == null) {
                return new VisualComparisonResult(0.0, "Unable to load image assets", false);
            }

            long hash1 = computeDifferenceHash(img1);
            long hash2 = computeDifferenceHash(img2);

            int hammingDistance = Long.bitCount(hash1 ^ hash2);
            // 64-bit hash: 0 distance = 100% identical, > 15 = distinct
            double similarity = Math.max(0.0, 1.0 - ((double) hammingDistance / 32.0));
            similarity = Math.round(similarity * 100.0) / 100.0;

            boolean clone = similarity >= 0.85;
            String assessment = clone ? "High visual likeness - possible cloned branding asset"
                    : (similarity >= 0.6 ? "Moderate visual similarity detected" : "Visually distinct");

            return new VisualComparisonResult(similarity, assessment, clone);

        } catch (Exception e) {
            log.debug("Visual image similarity calculation failed: {}", e.getMessage());
            return new VisualComparisonResult(0.0, "Analysis unavailable", false);
        }
    }

    private BufferedImage fetchAndNormalize(String urlStr) {
        try {
            URLConnection conn = URI.create(urlStr).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (InputStream in = conn.getInputStream()) {
                BufferedImage original = ImageIO.read(in);
                if (original == null) return null;

                // Scale to 9x8 grayscale image for difference hashing
                BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g = resized.createGraphics();
                g.drawImage(original.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();
                return resized;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private long computeDifferenceHash(BufferedImage img) {
        long hash = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = img.getRGB(x, y) & 0xFF;
                int right = img.getRGB(x + 1, y) & 0xFF;
                if (left > right) {
                    hash |= (1L << (y * 8 + x));
                }
            }
        }
        return hash;
    }
}
