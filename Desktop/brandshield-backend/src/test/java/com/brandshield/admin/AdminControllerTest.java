package com.brandshield.admin;

import com.brandshield.admin.dto.UpdateUserStatusRequest;
import com.brandshield.admin.repository.AuditLogRepository;
import com.brandshield.auth.dto.LoginRequest;
import com.brandshield.auth.dto.RegisterRequest;
import com.brandshield.security.CustomUserDetails;
import com.brandshield.security.jwt.JwtService;
import com.brandshield.user.entity.Role;
import com.brandshield.user.entity.User;
import com.brandshield.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AdminControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String normalUserToken;
    private String adminToken;
    private User normalUser;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Normal User (USER role)
        normalUser = new User();
        normalUser.setFullName("Regular User");
        normalUser.setEmail("user@brandshield.com");
        normalUser.setPassword(passwordEncoder.encode("Password@123"));
        normalUser.setRole(Role.USER);
        normalUser.setEnabled(true);
        normalUser = userRepository.save(normalUser);
        normalUserToken = jwtService.generateToken(new CustomUserDetails(normalUser));

        // 2. Create Administrator (ADMIN role)
        User adminUser = new User();
        adminUser.setFullName("System Admin");
        adminUser.setEmail("admin@brandshield.com");
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomUserDetails(adminUser));
    }

    @Test
    void testAdminSecurityAndOperations() throws Exception {
        // 1. Unauthenticated request to /admin -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());

        // 2. Normal USER attempting to access /admin/stats -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + normalUserToken))
                .andExpect(status().isForbidden());

        // 3. ADMIN accessing /admin/stats -> 200 OK
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.activeUsers").value(2));

        // 4. ADMIN lists all users
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // 5. ADMIN updates normal user status (disables user)
        UpdateUserStatusRequest updateReq = new UpdateUserStatusRequest(false, null);

        mockMvc.perform(patch("/api/v1/admin/users/" + normalUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // 6. Verify user in database is disabled
        User reloaded = userRepository.findById(normalUser.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();

        // 7. Verify an audit log was generated
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].action").value("UPDATE_USER_STATUS"));
    }
}
