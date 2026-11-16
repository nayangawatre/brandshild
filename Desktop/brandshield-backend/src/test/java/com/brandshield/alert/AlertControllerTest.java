package com.brandshield.alert;

import com.brandshield.alert.dto.UnreadCountResponse;
import com.brandshield.alert.entity.AlertStatus;
import com.brandshield.alert.repository.AlertRepository;
import com.brandshield.auth.dto.RegisterRequest;
import com.brandshield.brand.dto.CreateBrandRequest;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.threat.dto.AnalyzeTargetRequest;
import com.brandshield.threat.repository.ThreatRepository;
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
class AlertControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AlertRepository alertRepository;

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

        alertRepository.deleteAll();
        threatRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User 1
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Alert User", "user1@alerts.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("accessToken").asText();

        // 2. Register User 2
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Other User", "user2@alerts.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("accessToken").asText();

        // 3. User 1 creates a Brand
        CreateBrandRequest brandReq = new CreateBrandRequest(
                "Tesla",
                "Automotive",
                "https://www.tesla.com",
                "Electric vehicles & clean energy",
                null,
                Set.of("model s", "cybertruck", "autopilot"),
                Set.of("tesla.com")
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
    void testCompleteAlertLifecycle() throws Exception {
        // 1. Initially user has 0 unread alerts
        mockMvc.perform(get("/api/v1/alerts/unread-count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // 2. Perform on-demand analysis that flags a CRITICAL phishing threat
        AnalyzeTargetRequest analyzeReq = new AnalyzeTargetRequest(
                UUID.fromString(brand1Id),
                "https://tesla-account-verify-secure.xyz/login"
        );

        mockMvc.perform(post("/api/v1/threats/analyze")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analyzeReq)))
                .andExpect(status().isOk());

        // 3. Verify an alert was automatically triggered for User 1
        mockMvc.perform(get("/api/v1/alerts/unread-count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // 4. User 1 lists alerts
        MvcResult alertsResult = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.content[0].status").value("UNREAD"))
                .andExpect(jsonPath("$.content[0].brandName").value("Tesla"))
                .andReturn();

        String alertId = objectMapper.readTree(alertsResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();

        // 5. Tenant isolation: User 2 sees 0 alerts
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // 6. Tenant isolation: User 2 cannot mark User 1's alert as read (Expect 404)
        mockMvc.perform(patch("/api/v1/alerts/" + alertId + "/read")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());

        // 7. User 1 marks alert as read
        mockMvc.perform(patch("/api/v1/alerts/" + alertId + "/read")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // 8. Verify unread count is now 0
        mockMvc.perform(get("/api/v1/alerts/unread-count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // 9. User 1 marks all as read
        mockMvc.perform(patch("/api/v1/alerts/read-all")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All unread alerts marked as read"));

        // 10. User 1 dismisses alert
        mockMvc.perform(delete("/api/v1/alerts/" + alertId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());
    }
}
