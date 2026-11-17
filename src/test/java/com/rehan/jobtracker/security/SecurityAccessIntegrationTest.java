package com.rehan.jobtracker.security;

import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import com.rehan.jobtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security Access & JWT Filter Integration Tests")
class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User(
                "Secured User",
                "secured@example.com",
                passwordEncoder.encode("secretPassword123"),
                Role.USER
        );
        testUser = userRepository.save(testUser);
        validToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint /api/applications returns 401 Unauthorized")
    void unauthenticatedAccess_ToProtectedEndpoint_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("Authenticated request with valid Bearer token returns 200 OK")
    void authenticatedAccess_WithValidToken_ShouldReturn200Ok() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request with invalid JWT token returns 401 Unauthorized")
    void invalidJwt_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Request with expired JWT token returns 401 Unauthorized")
    void expiredJwt_ShouldReturn401Unauthorized() throws Exception {
        String expiredToken = jwtService.buildToken(Map.of(), testUser.getUsername(), -10000);

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Public auth endpoints are accessible without authentication")
    void publicAuthEndpoints_ShouldBePermittedWithoutToken() throws Exception {
        // /api/auth/register should be accessible (valid request returns 201)
        String registerJson = """
                {
                    "name": "New Person",
                    "email": "newperson@example.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is("newperson@example.com")));

        // /api/auth/login should be accessible (valid login returns 200)
        String loginJson = """
                {
                    "email": "secured@example.com",
                    "password": "secretPassword123"
                }
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is("secured@example.com")));
    }

    @Test
    @DisplayName("Regression Test: Public registration cannot create ADMIN users even if role:ADMIN is in payload")
    void publicRegistration_CannotEscalatePrivilegesToAdmin() throws Exception {
        String privilegeEscalationAttempt = """
                {
                    "name": "Attacker",
                    "email": "attacker@example.com",
                    "password": "password123",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(privilegeEscalationAttempt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("USER")));

        User savedUser = userRepository.findByEmail("attacker@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Admin endpoint is forbidden for normal users with ROLE_USER - returns 403 Forbidden")
    void adminEndpoint_ForbiddenForNormalUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/summary")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("insufficient permissions")));
    }

    @Test
    @DisplayName("Admin endpoint is accessible by administrator with ROLE_ADMIN - returns 200 OK")
    void adminEndpoint_AccessibleByAdmin_Returns200Ok() throws Exception {
        User adminUser = new User(
                "System Admin",
                "admin@example.com",
                passwordEncoder.encode("adminPass123"),
                Role.ADMIN
        );
        adminUser = userRepository.save(adminUser);
        String adminToken = jwtService.generateToken(adminUser);

        mockMvc.perform(get("/api/admin/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus", is("OPERATIONAL")))
                .andExpect(jsonPath("$.totalUsers").isNumber());
    }

    @Test
    @DisplayName("Admin endpoint without authentication returns 401 Unauthorized")
    void adminEndpoint_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}
