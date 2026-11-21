package com.rehan.jobtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class StartupDiagnosticsTest {

    @Test
    @DisplayName("StartupDiagnostics correctly reads active profiles and effective username")
    void shouldReadActiveProfilesAndEffectiveUsername() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require");
        env.setProperty("spring.datasource.username", "postgres.hmeufaqxjfjgjjcipsee");
        env.setProperty("spring.datasource.password", "supersecret");
        env.setProperty("jwt.secret", "a".repeat(64));

        StartupDiagnostics diagnostics = new StartupDiagnostics(env);
        diagnostics.printStartupDiagnostics();

        assertEquals("prod", diagnostics.getActiveProfiles());
        assertEquals("postgres.hmeufaqxjfjgjjcipsee", diagnostics.getEffectiveDatasourceUsername());
        assertEquals("jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require", diagnostics.getSanitizedDatasourceUrl());
    }

    @Test
    @DisplayName("StartupDiagnostics sanitizes credentials if present in JDBC URL")
    void shouldSanitizeCredentialsInJdbcUrl() {
        String sensitiveUrl = "jdbc:postgresql://postgres.hmeufaqxjfjgjjcipsee:secretpassword@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres";
        String sanitized = StartupDiagnostics.sanitizeJdbcUrl(sensitiveUrl);

        assertNotNull(sanitized);
        assertFalse(sanitized.contains("secretpassword"), "Sanitized URL must not contain password");
        assertTrue(sanitized.contains("postgres.hmeufaqxjfjgjjcipsee:****@"), "Sanitized URL should mask password");
    }

    @Test
    @DisplayName("StartupDiagnostics handles null URL gracefully")
    void shouldHandleNullUrl() {
        assertNull(StartupDiagnostics.sanitizeJdbcUrl(null));

        MockEnvironment env = new MockEnvironment();
        StartupDiagnostics diagnostics = new StartupDiagnostics(env);
        diagnostics.printStartupDiagnostics();

        assertEquals("default", diagnostics.getActiveProfiles());
        assertNull(diagnostics.getEffectiveDatasourceUsername());
        assertNull(diagnostics.getSanitizedDatasourceUrl());
    }
}
