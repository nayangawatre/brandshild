package com.brandshield.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class CertificateTransparencyService {

    private static final Logger log = LoggerFactory.getLogger(CertificateTransparencyService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record CtCertificateEntry(
            String domain,
            String issuerName,
            String notBefore,
            String notAfter
    ) {}

    public CertificateTransparencyService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Searches public Certificate Transparency logs for certificates issued containing brand name.
     */
    public List<CtCertificateEntry> searchCertificates(String brandKeyword, Set<String> officialDomains) {
        if (brandKeyword == null || brandKeyword.trim().length() < 3) {
            return List.of();
        }

        String query = "%" + brandKeyword.trim().toLowerCase() + "%";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://crt.sh/?q=" + encodedQuery + "&output=json";

        Set<String> seenDomains = new HashSet<>();
        List<CtCertificateEntry> results = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "BrandShield-CT-Monitor/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && !response.body().isBlank()) {
                JsonNode array = objectMapper.readTree(response.body());
                if (array.isArray()) {
                    for (JsonNode entry : array) {
                        String nameValue = entry.path("name_value").asText();
                        String issuer = entry.path("issuer_name").asText();
                        String notBefore = entry.path("not_before").asText();
                        String notAfter = entry.path("not_after").asText();

                        // name_value can contain multiple domains separated by newlines
                        for (String rawDomain : nameValue.split("\n")) {
                            String domain = rawDomain.trim().toLowerCase().replaceFirst("^\\*\\.", "");
                            if (domain.isBlank() || seenDomains.contains(domain)) continue;

                            // Skip official whitelisted domains
                            if (officialDomains != null && officialDomains.contains(domain)) continue;

                            seenDomains.add(domain);
                            results.add(new CtCertificateEntry(domain, issuer, notBefore, notAfter));

                            if (results.size() >= 20) break; // Limit top 20 findings
                        }
                        if (results.size() >= 20) break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Certificate Transparency log query skipped or timed out: {}", e.getMessage());
        }

        return results;
    }
}
