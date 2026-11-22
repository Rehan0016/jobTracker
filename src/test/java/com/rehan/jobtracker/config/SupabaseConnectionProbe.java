package com.rehan.jobtracker.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Safe standalone connection probe for Supabase PostgreSQL.
 *
 * Can be run completely independently of Spring Boot, HikariCP, and Hibernate
 * to verify network, SSL, credentials, and query execution against Supabase.
 *
 * NEVER prints the database password or secrets.
 */
public class SupabaseConnectionProbe {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("Supabase PostgreSQL Independent Connection & Query Test");
        System.out.println("==================================================================");

        Map<String, String> env = loadConfiguration();

        String url = env.getOrDefault("SPRING_DATASOURCE_URL",
                "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require");
        String username = env.getOrDefault("SPRING_DATASOURCE_USERNAME",
                "postgres.hmeufaqxjfjgjjcipsee");
        String password = env.get("SPRING_DATASOURCE_PASSWORD");

        // Safe diagnostics (never exposes password)
        boolean hasPassword = password != null && !password.isBlank();
        int passwordLen = hasPassword ? password.length() : 0;

        System.out.println("Target JDBC URL              : " + sanitizeJdbcUrl(url));
        System.out.println("Effective Datasource Username: " + username);
        System.out.println("Database Password Present    : " + hasPassword + " (length: " + passwordLen + " characters)");

        if ("your_supabase_db_password".equals(password)) {
            System.out.println("\n[CRITICAL WARNING]");
            System.out.println("The loaded password is the exact 25-character placeholder text: 'your_supabase_db_password'.");
            System.out.println("You must replace this placeholder in your .env file with your actual Supabase database password!");
        } else if (hasPassword && (password.startsWith("jdbc:") || password.startsWith("postgres://") || password.contains("://"))) {
            System.out.println("\n[CRITICAL WARNING]");
            System.out.println("The loaded password appears to be a full JDBC or database URL rather than a raw password!");
            System.out.println("Ensure SPRING_DATASOURCE_PASSWORD contains ONLY the password secret, not the connection string.");
        }

        if (!hasPassword) {
            System.out.println("\n[ERROR] SPRING_DATASOURCE_PASSWORD is empty or not set.");
            System.out.println("Set the variable in .env or via $env:SPRING_DATASOURCE_PASSWORD prior to running.");
            System.exit(1);
        }

        System.out.println("\nAttempting direct JDBC connection to Supabase pooler...");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] PostgreSQL JDBC Driver not found on classpath: " + e.getMessage());
            System.exit(1);
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("[SUCCESS] TCP connection and SSL handshake established!");
            System.out.println("[SUCCESS] Authenticated with Supabase pooler as: " + username);

            // Execute an actual query against the Supabase database
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT current_user, current_database(), version()")) {

                if (rs.next()) {
                    String currentUser = rs.getString(1);
                    String currentDb = rs.getString(2);
                    String version = rs.getString(3);

                    System.out.println("\n==================================================================");
                    System.out.println("REAL DATABASE QUERY RESULT:");
                    System.out.println("Current Database User : " + currentUser);
                    System.out.println("Current Database Name : " + currentDb);
                    System.out.println("PostgreSQL Version    : " + version);
                    System.out.println("==================================================================");
                    System.out.println("[SUCCESS] Database query executed cleanly! Supabase credentials are 100% verified.");
                }
            }
        } catch (SQLException e) {
            System.out.println("\n[CONNECTION FAILED]");
            System.out.println("SQLState     : " + e.getSQLState());
            System.out.println("Error Code   : " + e.getErrorCode());
            System.out.println("Server Error : " + e.getMessage());
            System.out.println("\nDIAGNOSIS:");
            if ("28P01".equals(e.getSQLState())) {
                System.out.println("Authentication failure (SQLState 28P01).");
                System.out.println("The password passed to Supabase is incorrect or rejected by the database backend.");
                System.out.println("Spring Boot configuration and username formatting are CORRECT.");
                System.out.println("To fix: Reset your database password in the Supabase Dashboard under Project Settings -> Database,");
                System.out.println("and update SPRING_DATASOURCE_PASSWORD with the new password.");
            } else if (e.getMessage() != null && e.getMessage().contains("no tenant identifier provided")) {
                System.out.println("Username format error: The pooler requires the tenant prefix (postgres.<project-ref>).");
            } else {
                System.out.println("Network or transport failure: Check internet connectivity and firewall rules.");
            }
            System.exit(2);
        }
    }

    private static String sanitizeJdbcUrl(String url) {
        if (url == null) return null;
        String sanitized = url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        return sanitized.replaceAll("(?i)([?&]password=)([^&]+)", "$1****");
    }

    private static Map<String, String> loadConfiguration() {
        Map<String, String> config = new HashMap<>();

        // 1. Read from OS process environment first
        for (String key : new String[]{"SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"}) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                config.put(key, val);
            }
        }

        // 2. Fall back to root .env file if not present in process environment
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.startsWith("#") && line.contains("=")) {
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String val = line.substring(eqIdx + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (!config.containsKey(key)) {
                            config.put(key, val);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return config;
    }
}
