package com.rehan.jobtracker.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Startup diagnostics component that logs active profile and database connection details
 * at startup without exposing sensitive secrets (such as database password or JWT secret).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;

    public StartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void printStartupDiagnostics() {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfileStr = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default (" + String.join(", ", environment.getDefaultProfiles()) + ")";

        String rawUrl = environment.getProperty("spring.datasource.url");
        String sanitizedUrl = sanitizeJdbcUrl(rawUrl);
        String username = environment.getProperty("spring.datasource.username");
        boolean passwordPresent = environment.containsProperty("spring.datasource.password")
                && environment.getProperty("spring.datasource.password") != null
                && !environment.getProperty("spring.datasource.password").isBlank();
        boolean jwtSecretPresent = environment.containsProperty("jwt.secret")
                && environment.getProperty("jwt.secret") != null
                && !environment.getProperty("jwt.secret").isBlank();

        log.info("==================================================================");
        log.info("JobTracker Startup Diagnostics");
        log.info("Active Profile(s)            : {}", activeProfileStr);
        log.info("Effective Datasource URL     : {}", sanitizedUrl != null ? sanitizedUrl : "(not set)");
        log.info("Effective Datasource Username: {}", username != null ? username : "(not set)");
        log.info("Database Password Configured : {}", passwordPresent);
        log.info("JWT Secret Configured        : {}", jwtSecretPresent);
        log.info("==================================================================");
    }

    public String getActiveProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default";
    }

    public String getEffectiveDatasourceUsername() {
        return environment.getProperty("spring.datasource.username");
    }

    public String getSanitizedDatasourceUrl() {
        return sanitizeJdbcUrl(environment.getProperty("spring.datasource.url"));
    }

    public static String sanitizeJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        // Mask embedded credentials in JDBC URL if present (e.g. jdbc:postgresql://user:pass@host:port/db)
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}
