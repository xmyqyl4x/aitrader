package com.myqyl.aitradex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfDockerAvailable
@ActiveProfiles("test")
class AitradexApplicationTests {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("aitradex_test")
          .withUsername("aitradex")
          .withPassword("aitradex");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void contextLoads() {
    // Ensures Spring context boots with Liquibase migrations against Testcontainers PostgreSQL.
  }
}
