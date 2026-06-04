package com.carsharing;

import com.carsharing.testutils.TestDataGenerator;
import com.carsharing.testutils.TestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("carsharing_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @BeforeAll
    public static void setupDatabase() {
        postgresContainer.start();
        System.setProperty("spring.datasource.url", postgresContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgresContainer.getUsername());
        System.setProperty("spring.datasource.password", postgresContainer.getPassword());
    }

    @AfterAll
    public static void teardownDatabase() {
        postgresContainer.stop();
    }

    protected final TestDataGenerator dataGenerator = new TestDataGenerator();
    protected final TestFixtures fixtures = new TestFixtures();
}