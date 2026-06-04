package com.carsharing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for PostgreSQL database using Testcontainers
 */
@Configuration
@Profile("test")
public class TestDatabaseConfig {

    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"))
                .withDatabaseName("carsharing_test")
                .withUsername("test_user")
                .withPassword("test_pass");
    }
}
