package com.brandshield.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class RdapLookupService {

    private static final Logger log = LoggerFactory.getLogger(RdapLookupService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record RdapResult(
            String domain,
            String registrar,
            String createdDate,
            String expirationDate,
            List<String> status,
            boolean isFound
    ) {}

    public RdapLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Look up domain registration data via RDAP protocol.
     */
    public RdapResult lookupDomain(String domain) {
        String clean = cleanDomain(domain);
        String url = "https://rdap.org/domain/" + clean;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/rdap+json, application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                String registrar = "Unknown";
                if (root.has("entities")) {
                    for (JsonNode entity : root.get("entities")) {
                        if (entity.has("roles")) {
                            for (JsonNode role : entity.get("roles")) {
                                if ("registrar".equalsIgnoreCase(role.asText())) {
                                    if (entity.has("vcardArray")) {
                                        registrar = extractVcardName(entity.get("vcardArray"));
                                    } else if (entity.has("handle")) {
                                        registrar = entity.get("handle").asText();
                                    }
                                }
                            }
                        }
                    }
                }

                String createdDate = "N/A";
                String expirationDate = "N/A";
                if (root.has("events")) {
                    for (JsonNode event : root.get("events")) {
                        String action = event.path("eventAction").asText();
                        if ("registration".equalsIgnoreCase(action)) {
                            createdDate = event.path("eventDate").asText();
                        } else if ("expiration".equalsIgnoreCase(action)) {
                            expirationDate = event.path("eventDate").asText();
                        }
                    }
                }

                List<String> statusList = new ArrayList<>();
                if (root.has("status")) {
                    for (JsonNode s : root.get("status")) {
                        statusList.add(s.asText());
                    }
                }

                return new RdapResult(clean, registrar, createdDate, expirationDate, statusList, true);
            }
        } catch (Exception e) {
            log.debug("RDAP query failed for {}: {}", clean, e.getMessage());
        }

        return new RdapResult(clean, "Unknown / Private", "N/A", "N/A", List.of("UNKNOWN"), false);
    }

    private String extractVcardName(JsonNode vcardArray) {
        try {
            if (vcardArray.isArray() && vcardArray.size() > 1) {
                JsonNode entries = vcardArray.get(1);
                for (JsonNode entry : entries) {
                    if (entry.isArray() && entry.size() > 3 && "fn".equalsIgnoreCase(entry.get(0).asText())) {
                        return entry.get(3).asText();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Unknown Registrar";
    }

    private String cleanDomain(String domain) {
        return domain.trim()
                .toLowerCase()
                .replaceFirst("^(https?://)?(www\\.)?", "")
                .split("/")[0]
                .split(":")[0];
    }
}
