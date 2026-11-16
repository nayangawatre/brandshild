package com.brandshield.auth;

import com.brandshield.auth.dto.LoginRequest;
import com.brandshield.auth.dto.RegisterRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @Test
    void testCompleteAuthenticationFlow() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
                "Rahul Patil",
                "rahul@gmail.com",
                "Rahul@123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.fullName").value("Rahul Patil"))
                .andExpect(jsonPath("$.user.email").value("rahul@gmail.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        // 2. Verify duplicate registration fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));

        // 3. Verify BCrypt password encryption in DB
        var savedUser = userRepository.findByEmail("rahul@gmail.com").orElseThrow();
        assertThat(savedUser.getPassword()).startsWith("$2a$");
        assertThat(savedUser.getPassword()).isNotEqualTo("Rahul@123");

        // 4. Login with valid credentials
        LoginRequest loginRequest = new LoginRequest("rahul@gmail.com", "Rahul@123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("rahul@gmail.com"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String jwtToken = jsonNode.get("accessToken").asText();

        // 5. Test protected /me endpoint WITHOUT token -> Expect 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // 6. Test protected /me endpoint WITH Bearer token -> Expect 200 OK
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Rahul Patil"))
                .andExpect(jsonPath("$.email").value("rahul@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
