package com.rehan.jobtracker.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Startup diagnostics and DataSource pre-initialization post-processor.
 *
 * Implements PriorityOrdered BeanPostProcessor to guarantee execution
 * immediately before the DataSource connection pool starts.
 *
 * Safe diagnostics: NEVER logs database passwords or JWT secrets.
 */
@Component
public class StartupDiagnostics implements BeanPostProcessor, PriorityOrdered, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);
    public static final String DEFAULT_POOLER_USER = "postgres.hmeufaqxjfjgjjcipsee";
    private static final Pattern USER_PARAM_PATTERN = Pattern.compile("([?&])user=([^&]+)");
    private static final Pattern CREDENTIALS_URL_PATTERN = Pattern.compile("://([^:]+):([^@]+)@");
    private static final Pattern PASSWORD_PARAM_PATTERN = Pattern.compile("([?&]password=)([^&]+)", Pattern.CASE_INSENSITIVE);

    private Environment environment;
    private volatile boolean dataSourceProcessed = false;
    private volatile String processedDataSourceUsername;
    private volatile String processedDataSourceUrl;

    public StartupDiagnostics() {
    }

    public StartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            this.dataSourceProcessed = true;
            String hikariUser = null;
            String hikariUrl = null;

            if (dataSource instanceof HikariDataSource hikari) {
                hikariUser = hikari.getUsername();
                hikariUrl = hikari.getJdbcUrl();

                // Safe runtime guard for Supabase Pooler:
                // If the URL or username contains bare 'postgres' on the pooler,
                // correct it to the full pooler username before the pool initializes.
                if (hikariUrl != null && hikariUrl.contains("pooler.supabase.com")) {
                    Matcher matcher = USER_PARAM_PATTERN.matcher(hikariUrl);
                    if (matcher.find()) {
                        String urlUser = matcher.group(2);
                        if ("postgres".equals(urlUser)) {
                            String correctedUrl = matcher.replaceFirst("$1user=" + DEFAULT_POOLER_USER);
                            hikari.setJdbcUrl(correctedUrl);
                            logAndPrint("WARNING: JDBC URL contained '?user=postgres' override! Corrected URL user to " + DEFAULT_POOLER_USER);
                            hikariUrl = correctedUrl;
                        }
                    }

                    if ("postgres".equals(hikariUser) || hikariUser == null || hikariUser.isBlank()) {
                        logAndPrint("WARNING: Effective database username on Supabase pooler was '" + hikariUser 
                                + "'! Overriding with pooler username '" + DEFAULT_POOLER_USER + "' before pool startup.");
                        hikari.setUsername(DEFAULT_POOLER_USER);
                        hikariUser = DEFAULT_POOLER_USER;
                    }
                }
            }

            this.processedDataSourceUsername = hikariUser;
            this.processedDataSourceUrl = hikariUrl;

            printDiagnostics("Immediately Before DataSource Initialization (HikariCP Pool Start)", hikariUser, hikariUrl);
        }
        return bean;
    }

    @PostConstruct
    public void printStartupDiagnostics() {
        printDiagnostics("PostConstruct Bean Instantiation", null, null);
    }

    public void printDiagnostics(String stage, String actualDataSourceUser, String actualDataSourceUrl) {
        if (environment == null) {
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfileStr = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default (" + String.join(", ", environment.getDefaultProfiles()) + ")";

        String envUrl = environment.getProperty("spring.datasource.url");
        String effectiveUrl = actualDataSourceUrl != null ? actualDataSourceUrl : envUrl;
        String sanitizedUrl = sanitizeJdbcUrl(effectiveUrl);

        String envUser = environment.getProperty("spring.datasource.username");
        String effectiveUser = actualDataSourceUser != null ? actualDataSourceUser : envUser;

        String rawPassword = environment.getProperty("spring.datasource.password");
        boolean passwordConfigured = rawPassword != null && !rawPassword.isBlank();
        int passwordLength = passwordConfigured ? rawPassword.length() : 0;
        boolean looksLikeUrl = passwordConfigured && (rawPassword.startsWith("jdbc:") || rawPassword.startsWith("postgres://") || rawPassword.contains("://"));

        String usernameSource = determinePropertySource("spring.datasource.username");

        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================================\n");
        sb.append(String.format("JobTracker Startup Diagnostics [%s]%n", stage));
        sb.append(String.format("Active Profile(s)            : %s%n", activeProfileStr));
        sb.append(String.format("Effective Datasource URL     : %s%n", sanitizedUrl != null ? sanitizedUrl : "(not set)"));
        sb.append(String.format("Effective Datasource Username: %s%n", effectiveUser != null ? effectiveUser : "(not set)"));
        sb.append(String.format("Username Configuration Source: %s%n", usernameSource));
        sb.append(String.format("Database Password Configured : %s (length: %d chars)%n", passwordConfigured, passwordLength));
        if (looksLikeUrl) {
            sb.append("WARNING                      : Database password appears to be a JDBC URL / connection string rather than a raw password!\n");
        }
        sb.append(String.format("JWT Secret Configured        : %s%n", environment.containsProperty("jwt.secret")));
        sb.append("==================================================================");

        logAndPrint(sb.toString());
    }

    public String determinePropertySource(String propertyName) {
        if (environment instanceof ConfigurableEnvironment configEnv) {
            for (PropertySource<?> source : configEnv.getPropertySources()) {
                if (source.containsProperty(propertyName)) {
                    Object val = source.getProperty(propertyName);
                    return String.format("%s (value: %s)", source.getName(), val);
                }
                String envVarName = propertyName.replace('.', '_').toUpperCase();
                if (source.containsProperty(envVarName)) {
                    Object val = source.getProperty(envVarName);
                    return String.format("%s [%s] (value: %s)", source.getName(), envVarName, val);
                }
            }
        }
        return "(unknown / default)";
    }

    private void logAndPrint(String message) {
        System.out.println(message);
        log.info(message);
    }

    public boolean isDataSourceProcessed() {
        return dataSourceProcessed;
    }

    public String getProcessedDataSourceUsername() {
        return processedDataSourceUsername;
    }

    public String getProcessedDataSourceUrl() {
        return processedDataSourceUrl;
    }

    public String getActiveProfiles() {
        if (environment == null) return "default";
        String[] activeProfiles = environment.getActiveProfiles();
        return (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default";
    }

    public String getEffectiveDatasourceUsername() {
        return environment != null ? environment.getProperty("spring.datasource.username") : null;
    }

    public String getSanitizedDatasourceUrl() {
        return sanitizeJdbcUrl(environment != null ? environment.getProperty("spring.datasource.url") : null);
    }

    public static String sanitizeJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        String sanitized = CREDENTIALS_URL_PATTERN.matcher(url).replaceAll("://$1:****@");
        sanitized = PASSWORD_PARAM_PATTERN.matcher(sanitized).replaceAll("$1****");
        return sanitized;
    }
}
