package com.myqyl.aitradex.etrade.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.client.EtradeAccountClient;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAccountAPI;
import com.myqyl.aitradex.etrade.client.EtradeApiClientMarketAPI;
import com.myqyl.aitradex.etrade.client.EtradeApiClientOrderAPI;
import com.myqyl.aitradex.etrade.client.EtradeOrderClient;
import com.myqyl.aitradex.etrade.client.EtradeQuoteClient;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for E*TRADE API integration tests.
 * 
 * These tests validate our application's E*TRADE integration layer by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying E*TRADE client calls
 * - Validating our request building, response parsing, error handling, etc.
 * 
 * Tests do NOT call E*TRADE's public endpoints directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class EtradeApiIntegrationTestBase {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("aitradex_test")
          .withUsername("aitradex")
          .withPassword("aitradex")
          .withReuse(true);

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @MockBean
  protected EtradeOrderClient orderClient;

  @MockBean
  protected EtradeQuoteClient quoteClient;

  @MockBean
  protected EtradeAccountClient accountClient;

  @MockBean
  protected EtradeApiClientAccountAPI accountsApi;

  @MockBean
  protected EtradeApiClientOrderAPI orderApi;

  @MockBean
  protected EtradeApiClientMarketAPI marketApi;

  @MockBean
  protected com.myqyl.aitradex.etrade.client.EtradeAlertsClient alertsClient;

  @Autowired
  protected EtradeAccountRepository accountRepository;

  @Autowired
  protected EtradeOAuthTokenRepository tokenRepository;

  protected UUID testAccountId;
  protected String testAccountIdKey = "12345678";
  protected UUID testUserId;

  @BeforeEach
  void setUpBase() {
    testUserId = UUID.randomUUID();
    testAccountId = UUID.randomUUID();

    // Create a test account in database
    EtradeAccount account = new EtradeAccount();
    account.setId(testAccountId);
    account.setUserId(testUserId);
    account.setAccountIdKey(testAccountIdKey);
    account.setAccountType("BROKERAGE");
    account.setAccountName("Test Account");
    account.setAccountStatus("ACTIVE");
    account.setLinkedAt(OffsetDateTime.now());
    accountRepository.save(account);
  }
}
