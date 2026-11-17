package com.brandshield.threat;

import com.brandshield.auth.dto.RegisterRequest;
import com.brandshield.brand.dto.CreateBrandRequest;
import com.brandshield.threat.dto.AnalyzeTargetRequest;
import com.brandshield.threat.dto.UpdateThreatStatusRequest;
import com.brandshield.threat.entity.ThreatStatus;
import com.brandshield.threat.repository.ThreatRepository;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ThreatControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ThreatRepository threatRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String user1Token;
    private String user2Token;
    private String brand1Id;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        threatRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User 1
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User One", "user1@threats.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("accessToken").asText();

        // 2. Register User 2
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User Two", "user2@threats.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("accessToken").asText();

        // 3. User 1 creates a Brand
        CreateBrandRequest brandReq = new CreateBrandRequest(
                "Apple",
                "Technology",
                "https://www.apple.com",
                "Consumer electronics & software",
                null,
                Set.of("iphone", "macbook", "ipad"),
                Set.of("apple.com", "icloud.com")
        );

        MvcResult brandRes = mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandReq)))
                .andExpect(status().isCreated())
                .andReturn();

        brand1Id = objectMapper.readTree(brandRes.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void testThreatDetectionLifecycle() throws Exception {
        // 1. Run automated scan on Apple brand
        MvcResult scanResult = mockMvc.perform(post("/api/v1/threats/scan/" + brand1Id)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode scanNode = objectMapper.readTree(scanResult.getResponse().getContentAsString());
        assertThat(scanNode.isArray()).isTrue();
        assertThat(scanNode.size()).isGreaterThan(0);

        // 2. Perform on-demand analysis of a phishing URL
        AnalyzeTargetRequest analyzeReq = new AnalyzeTargetRequest(
                UUID.fromString(brand1Id),
                "https://apple-id-verify-secure.xyz/login"
        );

        MvcResult analyzeResult = mockMvc.perform(post("/api/v1/threats/analyze")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analyzeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threatType").value("PHISHING"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.threatScore").value(org.hamcrest.Matchers.greaterThanOrEqualTo(80)))
                .andExpect(jsonPath("$.brandName").value("Apple"))
                .andReturn();

        String threatId = objectMapper.readTree(analyzeResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Fetch threats for brand (Paginated)
        mockMvc.perform(get("/api/v1/threats/brand/" + brand1Id)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(0)));

        // 4. Fetch threats filtered by CRITICAL severity
        mockMvc.perform(get("/api/v1/threats/brand/" + brand1Id + "?severity=CRITICAL")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"));

        // 5. Get threat by ID
        mockMvc.perform(get("/api/v1/threats/" + threatId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(threatId));

        // 6. User 2 cannot access User 1's threat (Tenant security isolation)
        mockMvc.perform(get("/api/v1/threats/" + threatId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Threat not found"));

        // 7. Update threat status to CONFIRMED
        UpdateThreatStatusRequest updateStatusReq = new UpdateThreatStatusRequest(ThreatStatus.CONFIRMED);

        mockMvc.perform(patch("/api/v1/threats/" + threatId + "/status")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 8. Fetch Threat Statistics for brand
        mockMvc.perform(get("/api/v1/threats/stats/" + brand1Id)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThreats").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.criticalCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.confirmedCount").value(1))
                .andExpect(jsonPath("$.averageThreatScore").value(org.hamcrest.Matchers.greaterThan(0.0)));

        // 9. Query Domain Intelligence (DNS, RDAP, Jsoup, Playwright, CT)
        mockMvc.perform(get("/api/v1/threats/intelligence?brandId=" + brand1Id + "&domain=apple-security.xyz")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("apple-security.xyz"))
                .andExpect(jsonPath("$.dns").exists())
                .andExpect(jsonPath("$.rdap").exists())
                .andExpect(jsonPath("$.contentAnalysis").exists())
                .andExpect(jsonPath("$.rendering").exists());
    }
}
