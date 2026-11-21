package com.rehan.jobtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {

    private Resource getMainResource(String filename) {
        File file = new File("src/main/resources/" + filename);
        if (file.exists()) {
            return new FileSystemResource(file);
        }
        return new FileSystemResource("target/classes/" + filename);
    }

    @Test
    @DisplayName("application.properties defaults active profile to prod")
    void applicationPropertiesDefaultsToProdProfile() throws IOException {
        Properties props = PropertiesLoaderUtils.loadProperties(getMainResource("application.properties"));
        String activeProfile = props.getProperty("spring.profiles.active");

        assertNotNull(activeProfile);
        assertEquals("${SPRING_PROFILES_ACTIVE:prod}", activeProfile,
                "Default active profile in application.properties must be prod");
    }

    @Test
    @DisplayName("application-prod.properties configures Supabase pooler datasource without hardcoded password")
    void applicationProdPropertiesConfiguration() throws IOException {
        Properties props = PropertiesLoaderUtils.loadProperties(getMainResource("application-prod.properties"));

        String url = props.getProperty("spring.datasource.url");
        String username = props.getProperty("spring.datasource.username");
        String password = props.getProperty("spring.datasource.password");
        String driver = props.getProperty("spring.datasource.driver-class-name");

        assertNotNull(url, "Datasource URL must be defined");
        assertTrue(url.contains("aws-1-ap-northeast-1.pooler.supabase.com:5432"),
                "URL should target the Supabase pooler host");
        assertTrue(url.contains("sslmode=require"),
                "URL should enforce sslmode=require");

        assertNotNull(username, "Datasource username must be defined");
        assertEquals("${SPRING_DATASOURCE_USERNAME:postgres.hmeufaqxjfjgjjcipsee}", username,
                "Datasource username must default to full pooler username 'postgres.hmeufaqxjfjgjjcipsee'");

        assertNotNull(password, "Datasource password property must be defined");
        assertEquals("${SPRING_DATASOURCE_PASSWORD}", password,
                "Datasource password must strictly be read from environment without default value fallback");

        assertEquals("org.postgresql.Driver", driver,
                "Driver should be PostgreSQL");
    }

    @Test
    @DisplayName("application-prod.properties has optimized pool settings for Supabase pooler")
    void applicationProdPoolSettings() throws IOException {
        Properties props = PropertiesLoaderUtils.loadProperties(getMainResource("application-prod.properties"));

        assertEquals("${DB_POOL_MAX:5}", props.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("${DB_POOL_MIN:1}", props.getProperty("spring.datasource.hikari.minimum-idle"));
        assertEquals("${SPRING_JPA_HIBERNATE_DDL_AUTO:update}", props.getProperty("spring.jpa.hibernate.ddl-auto"));
    }
}
