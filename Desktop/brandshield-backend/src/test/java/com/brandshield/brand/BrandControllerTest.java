package com.brandshield.brand;

import com.brandshield.auth.dto.LoginRequest;
import com.brandshield.auth.dto.RegisterRequest;
import com.brandshield.brand.dto.AddKeywordsRequest;
import com.brandshield.brand.dto.CreateBrandRequest;
import com.brandshield.brand.dto.UpdateBrandRequest;
import com.brandshield.brand.entity.BrandStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BrandControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        brandRepository.deleteAll();
        userRepository.deleteAll();

        // Register User 1
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User One", "user1@brandshield.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("accessToken").asText();

        // Register User 2
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User Two", "user2@brandshield.com", "Password@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        user2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void testCompleteBrandManagementLifecycle() throws Exception {
        // 1. Unauthenticated request should be rejected with 401
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isUnauthorized());

        // 2. User 1 creates a new brand
        CreateBrandRequest createRequest = new CreateBrandRequest(
                "Nike",
                "Footwear & Apparel",
                "https://www.nike.com",
                "Leading global athletic footwear and apparel brand",
                "https://cdn.example.com/nike.png",
                Set.of("air max", "jordan"),
                Set.of("nike.com", "nikeplus.com")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Nike"))
                .andExpect(jsonPath("$.industry").value("Footwear & Apparel"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.officialDomains").isArray())
                .andReturn();

        JsonNode brandJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String brandId = brandJson.get("id").asText();

        // 3. User 1 creating duplicate brand with same name fails with 400
        mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A brand with the name 'Nike' already exists"));

        // 4. User 2 can create a brand with the same name (isolation per user account)
        mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nike"));

        // 5. User 1 lists their brands (should have 1 brand)
        mockMvc.perform(get("/api/v1/brands")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Nike"));

        // 6. User 1 gets brand by ID
        mockMvc.perform(get("/api/v1/brands/" + brandId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(brandId))
                .andExpect(jsonPath("$.name").value("Nike"));

        // 7. User 2 tries to access User 1's brand -> Expect 404 Not Found (tenant security isolation)
        mockMvc.perform(get("/api/v1/brands/" + brandId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Brand not found"));

        // 8. User 1 updates brand details
        UpdateBrandRequest updateRequest = new UpdateBrandRequest(
                "Nike Global",
                "Sporting Goods",
                "https://www.nike.com",
                "Updated description",
                "https://cdn.example.com/nike2.png",
                BrandStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/brands/" + brandId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nike Global"))
                .andExpect(jsonPath("$.industry").value("Sporting Goods"));

        // 9. User 1 adds keywords
        com.brandshield.brand.dto.AddKeywordsRequest addKeywords = new com.brandshield.brand.dto.AddKeywordsRequest(
                Set.of("swoosh", "just do it")
        );

        mockMvc.perform(post("/api/v1/brands/" + brandId + "/keywords")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addKeywords)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray());

        // 10. User 1 deletes brand
        mockMvc.perform(delete("/api/v1/brands/" + brandId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // 11. Verify brand is deleted from DB
        mockMvc.perform(get("/api/v1/brands/" + brandId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }
}
