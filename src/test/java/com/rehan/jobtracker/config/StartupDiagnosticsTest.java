package com.rehan.jobtracker.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.Properties;

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
    @DisplayName("StartupDiagnostics sanitizes credentials in URL authority and query parameters")
    void shouldSanitizeCredentialsInJdbcUrl() {
        String authorityUrl = "jdbc:postgresql://postgres.hmeufaqxjfjgjjcipsee:secretpassword@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres";
        String sanitized1 = StartupDiagnostics.sanitizeJdbcUrl(authorityUrl);

        assertNotNull(sanitized1);
        assertFalse(sanitized1.contains("secretpassword"), "Sanitized URL must not contain password");
        assertTrue(sanitized1.contains("postgres.hmeufaqxjfjgjjcipsee:****@"), "Sanitized URL should mask password");

        String queryParamUrl = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?user=postgres&password=mySecretPass&sslmode=require";
        String sanitized2 = StartupDiagnostics.sanitizeJdbcUrl(queryParamUrl);

        assertFalse(sanitized2.contains("mySecretPass"), "Sanitized URL must not contain query password");
        assertTrue(sanitized2.contains("password=****"), "Sanitized URL should mask query password");
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

    @Test
    @DisplayName("Proves a JDBC URL containing user=postgres cannot override Supabase pooler username and correction happens before connection")
    void shouldPreventJdbcUrlUserOverrideAndCorrectBeforeConnection() {
        MockEnvironment env = new MockEnvironment();
        StartupDiagnostics diagnostics = new StartupDiagnostics(env);

        HikariDataSource ds = new HikariDataSource();
        String flawedUrl = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?user=postgres&sslmode=require";
        ds.setJdbcUrl(flawedUrl);
        ds.setUsername("postgres");

        // Verify pool has NOT started yet
        assertFalse(ds.isRunning(), "Hikari pool must not be started before postProcessBeforeInitialization");

        // Execute postProcessBeforeInitialization
        diagnostics.postProcessBeforeInitialization(ds, "dataSource");

        // Verify pool is STILL not running (modification occurs prior to any connection attempt)
        assertFalse(ds.isRunning(), "Hikari pool must remain unstarted during pre-initialization modification");

        // Verify DataSource configuration is safely updated
        assertEquals(StartupDiagnostics.DEFAULT_POOLER_USER, ds.getUsername(),
                "Hikari username must be corrected to Supabase pooler username");
        assertTrue(ds.getJdbcUrl().contains("user=" + StartupDiagnostics.DEFAULT_POOLER_USER),
                "JDBC URL query parameter user=postgres must be corrected to pooler username");
        assertFalse(ds.getJdbcUrl().contains("user=postgres&"),
                "JDBC URL must no longer contain bare user=postgres");

        // Verify the PostgreSQL Driver parses the corrected username from the updated URL
        Properties parsedProps = org.postgresql.Driver.parseURL(ds.getJdbcUrl(), new Properties());
        assertEquals(StartupDiagnostics.DEFAULT_POOLER_USER, parsedProps.getProperty("user"),
                "PostgreSQL driver must parse the corrected pooler username, proving URL cannot override with bare postgres");
    }

    @Test
    @DisplayName("StartupDiagnostics detects property source provenance")
    void shouldDetectPropertySource() {
        MockEnvironment env = new MockEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("renderEnvironment", Map.of(
                "spring.datasource.username", "postgres.hmeufaqxjfjgjjcipsee"
        )));

        StartupDiagnostics diagnostics = new StartupDiagnostics(env);
        String source = diagnostics.determinePropertySource("spring.datasource.username");

        assertTrue(source.contains("renderEnvironment"), "Should identify renderEnvironment as source");
        assertTrue(source.contains("postgres.hmeufaqxjfjgjjcipsee"), "Should identify value");
    }
}
