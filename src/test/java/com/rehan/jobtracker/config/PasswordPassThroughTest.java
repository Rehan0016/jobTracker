package com.rehan.jobtracker.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordPassThroughTest {

    @Test
    @DisplayName("Verify DataSourceProperties passes password unchanged to HikariDataSource without modification")
    void shouldPassPasswordUnchangedToHikariDataSource() {
        String testPassword = "Arbitrary_Secret_Password_2026!#$";

        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require");
        properties.setUsername("postgres.hmeufaqxjfjgjjcipsee");
        properties.setPassword(testPassword);
        properties.setDriverClassName("org.postgresql.Driver");

        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        assertEquals(testPassword.length(), ds.getPassword().length(),
                "Password length in HikariDataSource must exactly match the source property");
        assertEquals(testPassword, ds.getPassword(),
                "Password in HikariDataSource must be identical to the configured password property");
    }
}
