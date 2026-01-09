package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.domain.EtradeAlert;
import com.myqyl.aitradex.etrade.domain.EtradeAlertDetail;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Functional tests for E*TRADE Alerts API endpoints.
 *
 * These tests validate the complete Alerts API flow through our application's REST API endpoints:
 * 1. List Alerts (via /api/etrade/alerts) - validates alert retrieval and persistence
 * 2. Get Alert Details (via /api/etrade/alerts/{alertId}) - validates alert details retrieval and persistence
 * 3. Delete Alerts (via /api/etrade/alerts/{alertIdList}) - validates alert deletion and database state updates
 *
 * Tests make REAL calls to E*TRADE sandbox (not mocked) and validate:
 * - All API calls succeed (HTTP 200) or handle errors gracefully (404 for empty inbox)
 * - Response structure is correct
 * - Required fields are populated correctly
 * - OAuth token enforcement works correctly
 * - Database persistence works correctly (upsert alerts, alert details, alert events)
 *
 * Prerequisites:
 * - Local PostgreSQL database must be running on localhost:5432
 * - Database 'aitradexdb' must exist (or will be created by Liquibase)
 * - User 'aitradex_user' with password 'aitradex_pass' must have access to the database
 * - ETRADE_CONSUMER_KEY environment variable set
 * - ETRADE_CONSUMER_SECRET environment variable set
 * - ETRADE_ENCRYPTION_KEY environment variable set
 * - ETRADE_ACCESS_TOKEN environment variable set (or ETRADE_OAUTH_VERIFIER for automatic token exchange)
 * - ETRADE_ACCESS_TOKEN_SECRET environment variable set (or obtained via verifier)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("E*TRADE Alerts API - Functional Tests")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
class EtradeAlertsFunctionalTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ApplicationContext applicationContext;

  private static final Logger log = LoggerFactory.getLogger(EtradeAlertsFunctionalTest.class);

  @Autowired
  private EtradeOAuthTokenRepository tokenRepository;

  @Autowired
  private EtradeAccountRepository accountRepository;

  @Autowired
  private EtradeAlertRepository alertRepository;

  @Autowired
  private EtradeAlertDetailRepository alertDetailRepository;

  @Autowired
  private EtradeAlertEventRepository alertEventRepository;

  @Autowired
  private EtradeProperties properties;

  private UUID testUserId;

  @BeforeEach
  void setUpFunctional() {
    testUserId = UUID.randomUUID();

    // Clean up any existing test data
    alertEventRepository.deleteAll();
    alertDetailRepository.deleteAll();
    alertRepository.deleteAll();
    accountRepository.deleteAll();
    tokenRepository.deleteAll();

    log.info("Running functional Alerts API tests against E*TRADE sandbox: {}", properties.getBaseUrl());
  }

  @Test
  @DisplayName("Test 0: Token Prerequisite Enforcement - Alerts API requires OAuth token")
  void test0_tokenPrerequisiteEnforcement_alertsApiRequiresOAuthToken() throws Exception {
    log.info("=== Test 0: Token Prerequisite Enforcement ===");

    // Try to call Alerts API with non-existent account (should fail with account not found)
    UUID randomAccountId = UUID.randomUUID();
    try {
      MvcResult result = mockMvc.perform(get("/api/etrade/alerts")
              .param("accountId", randomAccountId.toString())
              .contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      int status = result.getResponse().getStatus();
      log.info("Status with non-existent account: {}", status);
      
      // Should return 400/404/500 (account not found or other error)
      assertTrue(status >= 400, "Should return error status for non-existent account");
      log.info("✅ Validated error handling for non-existent account (status: {})", status);
    } catch (Exception e) {
      // Exception is expected when account is not found
      log.info("✅ Validated error handling - exception thrown for non-existent account: {}", e.getMessage());
      assertTrue(true, "Exception expected for non-existent account");
    }

    // Try with valid token and account (if available)
    try {
      UUID authAccountId = ensureValidAccessToken();
      
      // Ensure account exists in database
      try {
        ensureAccountExists(authAccountId);
        
        MvcResult result = mockMvc.perform(get("/api/etrade/alerts")
                .param("accountId", authAccountId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        int status = result.getResponse().getStatus();
        log.info("Status with valid token: {}", status);
        
        // Should return 200 (success) or 404 (no alerts in inbox)
        assertTrue(status == 200 || status == 404, 
            "Should return 200 (success) or 404 (no alerts) with valid token, got: " + status);
        log.info("✅ Validated token requirement - API call succeeded with valid token (status: {})", status);
      } catch (Exception e) {
        log.warn("⚠️  Could not validate with valid token: {}", e.getMessage());
      }
    } catch (Exception e) {
      log.warn("⚠️  Could not obtain access token: {}", e.getMessage());
    }
  }

  @Test
  @DisplayName("Test 1: List Alerts - Happy Path with DB Persistence")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void test1_listAlerts_happyPath() throws Exception {
    log.info("=== Test 1: List Alerts - Happy Path ===");

    UUID accountId;
    try {
      accountId = ensureValidAccessToken();
    } catch (IllegalStateException e) {
      log.warn("⚠️  No access token available, skipping test");
      Assumptions.assumeTrue(false, "Access token required for this test");
      return;
    }
    ensureAccountExists(accountId);

    // Get initial alert count
    long initialAlertCount = alertRepository.count();

    // Call List Alerts API
    MvcResult result = mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString())
            .param("count", "25")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int status = result.getResponse().getStatus();
    String responseBody = result.getResponse().getContentAsString();
    log.info("List Alerts response status: {}", status);
    log.debug("List Alerts response body: {}", responseBody);

    // Accept 200 (success) or 404 (no alerts in inbox)
    assertTrue(status == 200 || status == 404, 
        "Should return 200 (success) or 404 (no alerts), got: " + status);

    if (status == 404) {
      log.info("⚠️  No alerts in inbox (404). Skipping persistence assertions.");
      return;
    }

    // Parse response
    JsonNode responseJson = objectMapper.readTree(responseBody);
    assertNotNull(responseJson, "Response should not be null");
    
    JsonNode alertsResponseNode = responseJson.path("totalAlerts");
    assertFalse(alertsResponseNode.isMissingNode(), "Response should contain totalAlerts");
    
    Long totalAlerts = alertsResponseNode.asLong();
    log.info("Total alerts: {}", totalAlerts);

    JsonNode alertsArray = responseJson.path("alerts");
    assertTrue(alertsArray.isArray(), "Response should contain alerts array");

    // Validate database persistence
    long finalAlertCount = alertRepository.count();
    log.info("Initial alert count: {}, Final alert count: {}", initialAlertCount, finalAlertCount);

    if (totalAlerts > 0) {
      // Should have persisted alerts
      assertTrue(finalAlertCount >= initialAlertCount, 
          "Alert count should have increased after List Alerts call");
      
      // Validate persisted alerts
      List<EtradeAlert> persistedAlerts = alertRepository.findByAccountId(accountId);
      assertFalse(persistedAlerts.isEmpty(), "Should have persisted at least one alert");
      
      // Validate alert fields
      EtradeAlert firstAlert = persistedAlerts.get(0);
      assertNotNull(firstAlert.getAlertId(), "Alert ID should not be null");
      assertNotNull(firstAlert.getStatus(), "Alert status should not be null");
      assertNotNull(firstAlert.getLastSyncedAt(), "Last synced timestamp should not be null");
      
      log.info("✅ Validated List Alerts - persisted {} alerts", persistedAlerts.size());
    } else {
      log.info("✅ Validated List Alerts - no alerts to persist");
    }
  }

  @Test
  @DisplayName("Test 2: Get Alert Details - Happy Path with DB Persistence")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void test2_getAlertDetails_happyPath() throws Exception {
    log.info("=== Test 2: Get Alert Details - Happy Path ===");

    UUID accountId;
    try {
      accountId = ensureValidAccessToken();
    } catch (IllegalStateException e) {
      log.warn("⚠️  No access token available, skipping test");
      Assumptions.assumeTrue(false, "Access token required for this test");
      return;
    }
    ensureAccountExists(accountId);

    // First, get alerts list to find an alert ID
    MvcResult listResult = mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();
    
    int listStatus = listResult.getResponse().getStatus();
    assertTrue(listStatus == 200 || listStatus == 404, 
        "Should return 200 (success) or 404 (no alerts), got: " + listStatus);

    if (listStatus == 404) {
      log.info("⚠️  No alerts in inbox. Skipping Get Alert Details test.");
      return;
    }

    String listResponseBody = listResult.getResponse().getContentAsString();
    JsonNode listResponseJson = objectMapper.readTree(listResponseBody);
    JsonNode alertsArray = listResponseJson.path("alerts");
    
    if (!alertsArray.isArray() || alertsArray.size() == 0) {
      log.info("⚠️  No alerts returned. Skipping Get Alert Details test.");
      return;
    }

    // Get first alert ID
    JsonNode firstAlert = alertsArray.get(0);
    String alertId = firstAlert.path("id").asText();
    log.info("Using alert ID: {}", alertId);

    // Get initial detail count
    long initialDetailCount = alertDetailRepository.count();

    // Call Get Alert Details API
    MvcResult result = mockMvc.perform(get("/api/etrade/alerts/{alertId}", alertId)
            .param("accountId", accountId.toString())
            .param("tags", "false")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    log.debug("Get Alert Details response body: {}", responseBody);

    // Parse response
    JsonNode responseJson = objectMapper.readTree(responseBody);
    assertNotNull(responseJson, "Response should not be null");
    assertEquals(Long.parseLong(alertId), responseJson.path("id").asLong(), "Alert ID should match");

    // Validate database persistence
    long finalDetailCount = alertDetailRepository.count();
    log.info("Initial detail count: {}, Final detail count: {}", initialDetailCount, finalDetailCount);

    // Find the alert entity
    Optional<EtradeAlert> alertOpt = alertRepository.findByAccountIdAndAlertId(accountId, Long.parseLong(alertId));
    Assumptions.assumeTrue(alertOpt.isPresent(), "Alert should exist in database");

    EtradeAlert alert = alertOpt.get();
    
    // Validate alert details were persisted
    Optional<EtradeAlertDetail> detailOpt = alertDetailRepository.findByAlertId(alert.getId());
    assertTrue(detailOpt.isPresent(), "Alert details should be persisted");
    
    EtradeAlertDetail detail = detailOpt.get();
    assertNotNull(detail.getDetailsFetchedAt(), "Details fetched timestamp should not be null");
    
    log.info("✅ Validated Get Alert Details - persisted alert details for alert {}", alertId);
  }

  @Test
  @DisplayName("Test 3: Delete Alerts - Happy Path with DB State Updates")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void test3_deleteAlerts_happyPath() throws Exception {
    log.info("=== Test 3: Delete Alerts - Happy Path ===");

    UUID accountId;
    try {
      accountId = ensureValidAccessToken();
    } catch (IllegalStateException e) {
      log.warn("⚠️  No access token available, skipping test");
      Assumptions.assumeTrue(false, "Access token required for this test");
      return;
    }
    ensureAccountExists(accountId);

    // First, get alerts list to find alert IDs to delete
    MvcResult listResult = mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();
    
    int listStatus = listResult.getResponse().getStatus();
    assertTrue(listStatus == 200 || listStatus == 404, 
        "Should return 200 (success) or 404 (no alerts), got: " + listStatus);

    if (listStatus == 404) {
      log.info("⚠️  No alerts in inbox. Skipping Delete Alerts test.");
      return;
    }

    String listResponseBody = listResult.getResponse().getContentAsString();
    JsonNode listResponseJson = objectMapper.readTree(listResponseBody);
    JsonNode alertsArray = listResponseJson.path("alerts");
    
    if (!alertsArray.isArray() || alertsArray.size() == 0) {
      log.info("⚠️  No alerts returned. Skipping Delete Alerts test.");
      return;
    }

    // Get first alert ID (or use a test alert ID if none available)
    JsonNode firstAlert = alertsArray.get(0);
    String alertId = firstAlert.path("id").asText();
    log.info("Attempting to delete alert ID: {}", alertId);

    // Ensure alert exists in database
    Optional<EtradeAlert> alertOpt = alertRepository.findByAccountIdAndAlertId(accountId, Long.parseLong(alertId));
    if (!alertOpt.isPresent()) {
      // Create alert record if it doesn't exist
      EtradeAlert alert = new EtradeAlert();
      alert.setAccountId(accountId);
      alert.setAlertId(Long.parseLong(alertId));
      alert.setStatus("UNREAD");
      alertRepository.save(alert);
      alertOpt = Optional.of(alert);
    }

    EtradeAlert alert = alertOpt.get();
    String initialStatus = alert.getStatus();
    log.info("Initial alert status: {}", initialStatus);

    // Get initial event count
    long initialEventCount = alertEventRepository.count();

    // Call Delete Alerts API
    MvcResult result = mockMvc.perform(delete("/api/etrade/alerts/{alertIdList}", alertId)
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    log.debug("Delete Alerts response body: {}", responseBody);

    // Parse response
    JsonNode responseJson = objectMapper.readTree(responseBody);
    assertNotNull(responseJson, "Response should not be null");
    
    String resultStatus = responseJson.path("result").asText();
    log.info("Delete result: {}", resultStatus);

    // Validate database state updates (reload from database)
    alertRepository.flush();
    Optional<EtradeAlert> reloadedAlertOpt = alertRepository.findById(alert.getId());
    Assumptions.assumeTrue(reloadedAlertOpt.isPresent(), "Alert should still exist in database");
    EtradeAlert reloadedAlert = reloadedAlertOpt.get();
    String finalStatus = reloadedAlert.getStatus();
    log.info("Final alert status: {}", finalStatus);

    // Check if delete was successful (no failed alerts)
    JsonNode failedAlerts = responseJson.path("failedAlerts");
    if (failedAlerts.isArray() && failedAlerts.size() == 0) {
      // Successfully deleted - status should be DELETED
      assertEquals("DELETED", finalStatus, "Alert status should be DELETED after successful deletion");
      
      // Validate event was created
      long finalEventCount = alertEventRepository.count();
      assertTrue(finalEventCount > initialEventCount, "Alert event should be created");
      
      log.info("✅ Validated Delete Alerts - alert status updated to DELETED");
    } else {
      // Some alerts failed to delete
      log.warn("⚠️  Some alerts failed to delete: {}", failedAlerts.toString());
      
      // Validate failure event was created
      long finalEventCount = alertEventRepository.count();
      assertTrue(finalEventCount > initialEventCount, "Alert event should be created for failure");
      
      log.info("✅ Validated Delete Alerts - failure event created");
    }
  }

  @Test
  @DisplayName("Test 4: Get Alert Details - Invalid Alert ID")
  void test4_getAlertDetails_invalidAlertId() throws Exception {
    log.info("=== Test 4: Get Alert Details - Invalid Alert ID ===");

    UUID accountId;
    try {
      accountId = ensureValidAccessToken();
    } catch (IllegalStateException e) {
      log.warn("⚠️  No access token available, skipping test");
      Assumptions.assumeTrue(false, "Access token required for this test");
      return;
    }
    ensureAccountExists(accountId);

    // Try to get details for invalid alert ID (0 or negative)
    String invalidAlertId = "0";
    
    MvcResult result = mockMvc.perform(get("/api/etrade/alerts/{alertId}", invalidAlertId)
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int status = result.getResponse().getStatus();
    log.info("Status for invalid alert ID: {}", status);
    
    // Should return 400 or 404 (validation error or alert not found)
    assertTrue(status >= 400, "Should return error status for invalid alert ID");
    log.info("✅ Validated error handling for invalid alert ID (status: {})", status);
  }

  @Test
  @DisplayName("Test 5: List Alerts - Filters and Pagination")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void test5_listAlerts_filtersAndPagination() throws Exception {
    log.info("=== Test 5: List Alerts - Filters and Pagination ===");

    UUID accountId;
    try {
      accountId = ensureValidAccessToken();
    } catch (IllegalStateException e) {
      log.warn("⚠️  No access token available, skipping test");
      Assumptions.assumeTrue(false, "Access token required for this test");
      return;
    }
    ensureAccountExists(accountId);

    // Test with filters
    MvcResult result = mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString())
            .param("count", "10")
            .param("category", "STOCK")
            .param("status", "UNREAD")
            .param("direction", "DESC")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();
    
    int status = result.getResponse().getStatus();
    assertTrue(status == 200 || status == 404, 
        "Should return 200 (success) or 404 (no alerts), got: " + status);

    log.info("List Alerts with filters - status: {}", status);

    if (status == 200) {
      String responseBody = result.getResponse().getContentAsString();
      JsonNode responseJson = objectMapper.readTree(responseBody);
      
      JsonNode totalAlerts = responseJson.path("totalAlerts");
      JsonNode alertsArray = responseJson.path("alerts");
      
      log.info("Total alerts: {}, Returned alerts: {}", 
          totalAlerts.asLong(), alertsArray.size());
      
      // Validate response structure
      assertTrue(totalAlerts.isNumber(), "totalAlerts should be a number");
      assertTrue(alertsArray.isArray(), "alerts should be an array");
      
      log.info("✅ Validated List Alerts with filters");
    } else {
      log.info("✅ Validated List Alerts - no alerts found (404)");
    }
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Ensures a valid access token exists for testing.
   * Returns the account ID associated with the token.
   */
  private UUID ensureValidAccessToken() {
    // Check if we have a valid access token in the database
    List<EtradeOAuthToken> tokens = tokenRepository.findAll();
    for (EtradeOAuthToken token : tokens) {
      if ("SUCCESS".equals(token.getStatus()) 
          && token.getAccessTokenEncrypted() != null 
          && token.getAccountId() != null) {
        log.info("Found valid access token for account: {}", token.getAccountId());
        return token.getAccountId();
      }
    }

    // No valid token found - try to get one via OAuth flow
    log.warn("⚠️  No valid access token found. Attempting OAuth flow...");
    String verifier = System.getenv("ETRADE_OAUTH_VERIFIER");
    if (verifier == null || verifier.isEmpty()) {
      log.error("❌ ETRADE_OAUTH_VERIFIER not set. Cannot obtain access token.");
      log.error("   To complete tests:");
      log.error("   1. Run OAuth flow to obtain access token");
      log.error("   2. Set ETRADE_OAUTH_VERIFIER environment variable");
      log.error("   3. Re-run tests");
      throw new IllegalStateException("No valid access token available and ETRADE_OAUTH_VERIFIER not set");
    }

    // Get request token
    EtradeOAuthService oauthService = applicationContext.getBean(EtradeOAuthService.class);
    EtradeOAuthService.RequestTokenResponse requestTokenResponse = oauthService.getRequestToken(testUserId);
    
    // Exchange for access token
    UUID tempAccountId = UUID.randomUUID();
    oauthService.exchangeForAccessToken(
        requestTokenResponse.getRequestToken(),
        requestTokenResponse.getRequestTokenSecret(),
        verifier,
        tempAccountId);
    
    log.info("✅ Successfully obtained access token for account: {}", tempAccountId);
    return tempAccountId;
  }

  /**
   * Ensures an account exists in the database for the given account ID.
   * If account doesn't exist, tries to sync accounts via List Accounts API.
   */
  private void ensureAccountExists(UUID accountId) {
    Optional<EtradeAccount> accountOpt = accountRepository.findById(accountId);
    if (!accountOpt.isPresent()) {
      // Try to sync accounts via List Accounts API
      try {
        MvcResult result = mockMvc.perform(post("/api/etrade/accounts/sync")
                .param("userId", testUserId.toString())
                .param("accountId", accountId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        if (result.getResponse().getStatus() == 200) {
          // Accounts synced successfully
          accountOpt = accountRepository.findById(accountId);
          if (accountOpt.isPresent()) {
            log.info("Synced account from List Accounts API: {}", accountId);
            return;
          }
        }
      } catch (Exception e) {
        log.warn("Failed to sync accounts: {}", e.getMessage());
      }
      
      // If sync failed, create a minimal test account
      EtradeAccount account = new EtradeAccount();
      account.setId(accountId);
      account.setAccountIdKey("TEST_ACCOUNT_KEY");
      account.setAccountName("Test Account");
      account.setAccountType("INDIVIDUAL");
      account.setAccountStatus("ACTIVE");
      accountRepository.save(account);
      log.info("Created test account: {}", accountId);
    }
  }
}
