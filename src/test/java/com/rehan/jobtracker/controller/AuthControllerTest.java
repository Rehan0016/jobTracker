package com.rehan.jobtracker.controller;

import com.rehan.jobtracker.dto.AuthResponse;
import com.rehan.jobtracker.dto.LoginRequest;
import com.rehan.jobtracker.dto.RegisterRequest;
import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.exception.DuplicateEmailException;
import com.rehan.jobtracker.exception.GlobalExceptionHandler;
import com.rehan.jobtracker.security.CustomUserDetailsService;
import com.rehan.jobtracker.security.JwtService;
import com.rehan.jobtracker.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - Successfully register new user (201 Created)")
    void register_Success_ShouldReturn201CreatedAndAuthResponse() throws Exception {
        AuthResponse mockResponse = new AuthResponse(
                "mock.jwt.token.123",
                "Bearer",
                1L,
                "Jane Doe",
                "jane@example.com",
                Role.USER
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                    "name": "Jane Doe",
                    "email": "jane@example.com",
                    "password": "securePassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("mock.jwt.token.123")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane@example.com")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject duplicate email (400 Bad Request)")
    void register_DuplicateEmail_ShouldReturn400BadRequest() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Email is already registered: duplicate@example.com"));

        String json = """
                {
                    "name": "Existing User",
                    "email": "duplicate@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("duplicate@example.com")))
                .andExpect(jsonPath("$.path", is("/api/auth/register")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject invalid inputs (blank name, invalid email, short password)")
    void register_InvalidInputs_ShouldReturn400WithFieldErrors() throws Exception {
        String invalidJson = """
                {
                    "name": "   ",
                    "email": "not-an-email",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/login - Successfully login (200 OK)")
    void login_Success_ShouldReturn200OkAndAuthResponse() throws Exception {
        AuthResponse mockResponse = new AuthResponse(
                "mock.login.jwt.token",
                "Bearer",
                1L,
                "Jane Doe",
                "jane@example.com",
                Role.USER
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                    "email": "jane@example.com",
                    "password": "securePassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mock.login.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Reject invalid credentials (401 Unauthorized)")
    void login_BadCredentials_ShouldReturn401Unauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        String json = """
                {
                    "email": "jane@example.com",
                    "password": "wrongPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Reject blank email or password (400 Bad Request)")
    void login_BlankInputs_ShouldReturn400BadRequest() throws Exception {
        String invalidJson = """
                {
                    "email": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }
}
