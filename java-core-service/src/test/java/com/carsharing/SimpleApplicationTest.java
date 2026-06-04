package com.carsharing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify the application starts correctly
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class SimpleApplicationTest {

    @Test
    void contextLoads() {
        // Simple test to verify Spring context loads
        assertThat(true).isTrue();
    }
    
    @Test
    void applicationStarts() {
        // Test that the application can start
        CarsharingApplication.main(new String[]{});
    }
}