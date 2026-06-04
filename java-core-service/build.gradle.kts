plugins {
    java
    id("org.springframework.boot") version "3.4.2" // Стабильная версия с поддержкой Java 21-25
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web API для создания эндпоинтов каршеринга
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Security for PasswordEncoder
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ORM для работы с PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Драйвер PostgreSQL
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    // Миграции базы данных
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JSON processing for telemetry data
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Structured logging with Logback JSON encoder
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Утилита Lombok для генерации геттеров/сеттеров
    // compileOnly("org.projectlombok:lombok:1.18.36")
    // annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Тестирование
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.4")
    
    // Test containers for integration testing
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    
    // Spring Boot Actuator for health checks and monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}