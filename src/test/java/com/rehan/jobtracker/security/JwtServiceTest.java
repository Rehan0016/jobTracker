package com.rehan.jobtracker.security;

import com.rehan.jobtracker.entity.Role;
import com.rehan.jobtracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    // 256-bit secret key for testing (64 hex characters)
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, TEST_EXPIRATION_MS);
        testUser = new User("Alice Developer", "alice@example.com", "encodedPassword123", Role.USER);
        testUser.setId(1L);
    }

    @Test
    @DisplayName("generateToken and extractUsername should return the user email")
    void generateToken_ShouldReturnValidTokenWithUserEmailAsSubject() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String extractedEmail = jwtService.extractUsername(token);
        assertEquals("alice@example.com", extractedEmail);
    }

    @Test
    @DisplayName("isTokenValid should return true for valid token and matching user")
    void isTokenValid_WithMatchingUser_ShouldReturnTrue() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("isTokenValid should return false when username does not match")
    void isTokenValid_WithDifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);

        User anotherUser = new User("Bob", "bob@example.com", "encodedPassword456", Role.USER);
        anotherUser.setId(2L);

        boolean isValid = jwtService.isTokenValid(token, anotherUser);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("isTokenValid and isTokenExpired should handle expired tokens correctly")
    void isTokenExpired_WithExpiredToken_ShouldReturnTrueAndInvalid() {
        // Build a token that expired 10 seconds ago
        String expiredToken = jwtService.buildToken(Map.of(), testUser.getUsername(), -10000);

        assertTrue(jwtService.isTokenExpired(expiredToken));
        assertFalse(jwtService.isTokenValid(expiredToken, testUser));
    }

    @Test
    @DisplayName("isTokenValid should return false for malformed token string")
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        assertFalse(jwtService.isTokenValid("invalid.malformed.token", testUser));
        assertFalse(jwtService.isTokenValid("", testUser));
        assertFalse(jwtService.isTokenValid(null, testUser));
    }

    @Test
    @DisplayName("isTokenValid should return false for token with tampered signature")
    void isTokenValid_WithTamperedSignature_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);
        // Tamper with the token by changing the last characters
        String tamperedToken = token.substring(0, token.length() - 6) + "xxxxxx";

        assertFalse(jwtService.isTokenValid(tamperedToken, testUser));
    }

    @Test
    @DisplayName("extractClaim should correctly extract custom claims")
    void extractClaim_ShouldExtractCustomClaims() {
        Map<String, Object> extraClaims = Map.of("role", "ROLE_USER", "userId", 1L);
        String token = jwtService.generateToken(extraClaims, testUser);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("ROLE_USER", role);
    }
}
