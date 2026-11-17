package com.rehan.jobtracker.controller;

import com.rehan.jobtracker.dto.AuthResponse;
import com.rehan.jobtracker.dto.LoginRequest;
import com.rehan.jobtracker.dto.RegisterRequest;
import com.rehan.jobtracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Constructor-based dependency injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register - Register a new user account
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * POST /api/auth/login - Authenticate existing user and retrieve JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
