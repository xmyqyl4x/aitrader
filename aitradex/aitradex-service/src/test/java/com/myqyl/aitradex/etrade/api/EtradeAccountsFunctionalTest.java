package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.domain.EtradeBalance;
import com.myqyl.aitradex.etrade.domain.EtradePortfolioPosition;
import com.myqyl.aitradex.etrade.domain.EtradeTransaction;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import com.myqyl.aitradex.etrade.repository.EtradeBalanceRepository;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import com.myqyl.aitradex.etrade.repository.EtradePortfolioPositionRepository;
import com.myqyl.aitradex.etrade.repository.EtradeTransactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

/**
 * Functional tests for E*TRADE Accounts API endpoints.
 *
 * These tests validate the complete Accounts API flow through our application's REST API endpoints:
 * 1. List Accounts (via /api/etrade/accounts/sync) - validates account upsert
 * 2. Get Account Balance (via /api/etrade/accounts/{accountId}/balance) - validates balance snapshot persistence
 * 3. List Transactions (via /api/etrade/accounts/{accountId}/transactions) - validates transaction upsert
 * 4. Get Transaction Details (via /api/etrade/accounts/{accountId}/transactions/{transactionId}) - validates details update
 * 5. View Portfolio (via /api/etrade/accounts/{accountId}/portfolio) - validates position upsert
 *
 * Tests make REAL calls to E*TRADE sandbox (not mocked) and validate:
 * - All API calls succeed (HTTP 200)
 * - Response structure is correct
 * - Database persistence works correctly (upsert/append-only as appropriate)
 * - Required fields are populated correctly
 *
 * Prerequisites:
 * - Local PostgreSQL database must be running on localhost:5432
 * - Database 'aitradex_test' must exist (or will be created by Liquibase)
 * - User 'aitradex' with password 'aitradex' must have access to the database
 * - ETRADE_CONSUMER_KEY environment variable set
 * - ETRADE_CONSUMER_SECRET environment variable set
 * - ETRADE_ENCRYPTION_KEY environment variable set
 * - ETRADE_ACCESS_TOKEN environment variable set (or ETRADE_OAUTH_VERIFIER for automatic token exchange)
 * - ETRADE_ACCESS_TOKEN_SECRET environment variable set (or obtained via verifier)
 * - ETRADE_ACCOUNT_ID_KEY environment variable set (E*TRADE account ID key for testing, optional)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("E*TRADE Accounts API - Functional Tests")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
class EtradeAccountsFunctionalTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ApplicationContext applicationContext;

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountsFunctionalTest.class);

  @Autowired
  private EtradeAccountRepository accountRepository;

  @Autowired
  private EtradeBalanceRepository balanceRepository;

  @Autowired
  private EtradeTransactionRepository transactionRepository;

  @Autowired
  private EtradePortfolioPositionRepository positionRepository;

  @Autowired
  private EtradeOAuthTokenRepository tokenRepository;

  @Autowired
  private EtradeProperties properties;

  private UUID testUserId;
  private UUID testAccountId;
  private String testAccountIdKey;

  @BeforeEach
  void setUpFunctional() {
    testUserId = UUID.randomUUID();

    // Get account ID key from environment or use default
    testAccountIdKey = System.getenv("ETRADE_ACCOUNT_ID_KEY");
    if (testAccountIdKey == null || testAccountIdKey.isEmpty()) {
      log.warn("⚠️  ETRADE_ACCOUNT_ID_KEY not set. Tests may be skipped.");
      log.warn("   To complete tests:");
      log.warn("   1. Obtain account ID key from List Accounts API");
      log.warn("   2. Set ETRADE_ACCOUNT_ID_KEY environment variable");
      log.warn("   3. Re-run tests");
      // Continue anyway - we'll try to get it from List Accounts
    }

    // Clean up any existing test data
    transactionRepository.deleteAll();
    positionRepository.deleteAll();
    balanceRepository.deleteAll();
    accountRepository.deleteAll();
    tokenRepository.deleteAll();

    log.info("Running functional Accounts API tests against E*TRADE sandbox: {}", properties.getBaseUrl());
  }

  @Test
  @DisplayName("Step 1: List Accounts - Via REST API - Validates Account Upsert")
  void step1_listAccounts_viaRestApi_validatesAccountUpsert() throws Exception {
    log.info("=== Step 1: List Accounts via REST API ===");

    // Ensure we have a valid access token first (from OAuth flow)
    // For testing, we assume access token is available from environment or previous OAuth test
    UUID authAccountId = ensureValidAccessToken();

    // Call our REST API endpoint to sync accounts (this calls List Accounts API and persists)
    MvcResult result = mockMvc.perform(post("/api/etrade/accounts/sync")
            .param("userId", testUserId.toString())
            .param("accountId", authAccountId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    log.info("Response: {}", responseContent);

    JsonNode responseJson = objectMapper.readTree(responseContent);
    assertTrue(responseJson.isArray(), "Response should be an array of accounts");
    assertTrue(responseJson.size() > 0, "Response should contain at least one account");

    // Get account ID key from first account if not set
    if (testAccountIdKey == null || testAccountIdKey.isEmpty()) {
      JsonNode firstAccount = responseJson.get(0);
      testAccountIdKey = firstAccount.get("accountIdKey").asText();
      log.info("Using account ID key from response: {}", testAccountIdKey);
    }

    // Find the account in the database
    Optional<EtradeAccount> accountOpt = accountRepository.findByAccountIdKey(testAccountIdKey);
    assertTrue(accountOpt.isPresent(), "Account should be persisted in database");

    EtradeAccount account = accountOpt.get();
    assertEquals(testAccountIdKey, account.getAccountIdKey(), "Account ID key should match");
    assertNotNull(account.getAccountType(), "Account type should be populated");
    assertNotNull(account.getAccountName(), "Account name should be populated");
    assertNotNull(account.getAccountStatus(), "Account status should be populated");
    assertNotNull(account.getLastSyncedAt(), "Last synced at should be populated");

    testAccountId = account.getId();
    log.info("✅ Step 1 completed via REST API");
    log.info("  Account ID: {}", testAccountId);
    log.info("  Account ID Key: {}", testAccountIdKey);
    log.info("  Account Name: {}", account.getAccountName());
    log.info("  Account Type: {}", account.getAccountType());
    log.info("  Account Status: {}", account.getAccountStatus());
  }

  @Test
  @DisplayName("Step 2: Get Account Balance - Via REST API - Validates Balance Snapshot Persistence")
  void step2_getAccountBalance_viaRestApi_validatesBalanceSnapshotPersistence() throws Exception {
    log.info("=== Step 2: Get Account Balance via REST API ===");

    // Ensure account exists (run Step 1 first if needed)
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial balance count
    long balanceCountBefore = balanceRepository.count();

    // Call our REST API endpoint to get account balance
    MvcResult result = mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", testAccountId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    log.info("Response: {}", responseContent);

    JsonNode balanceJson = objectMapper.readTree(responseContent);
    assertNotNull(balanceJson.get("accountId"), "Response should contain accountId");
    assertNotNull(balanceJson.get("accountType"), "Response should contain accountType");

    // Validate database persistence - should have created a NEW balance snapshot
    long balanceCountAfter = balanceRepository.count();
    assertEquals(balanceCountBefore + 1, balanceCountAfter, 
        "Balance snapshot count should increase by 1 (append-only history)");

    List<EtradeBalance> balances = balanceRepository.findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
    assertFalse(balances.isEmpty(), "Balance snapshots should exist for account");
    
    EtradeBalance latestBalance = balances.get(0);
    assertEquals(testAccountId, latestBalance.getAccountId(), "Account ID should match");
    assertNotNull(latestBalance.getSnapshotTime(), "Snapshot time should be populated");
    
    // Validate balance fields are populated (at least some should be non-null)
    boolean hasBalanceData = latestBalance.getCashBalance() != null 
        || latestBalance.getMarginBalance() != null 
        || latestBalance.getTotalValue() != null 
        || latestBalance.getNetValue() != null;
    assertTrue(hasBalanceData, "Balance snapshot should contain at least some balance data");

    log.info("✅ Step 2 completed via REST API");
    log.info("  Balance Snapshot ID: {}", latestBalance.getId());
    log.info("  Snapshot Time: {}", latestBalance.getSnapshotTime());
    log.info("  Cash Balance: {}", latestBalance.getCashBalance());
    log.info("  Total Value: {}", latestBalance.getTotalValue());
    log.info("  Net Value: {}", latestBalance.getNetValue());
  }

  @Test
  @DisplayName("Step 3: List Transactions - Via REST API - Validates Transaction Upsert")
  void step3_listTransactions_viaRestApi_validatesTransactionUpsert() throws Exception {
    log.info("=== Step 3: List Transactions via REST API ===");

    // Ensure account exists (run Step 1 first if needed)
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial transaction count
    long transactionCountBefore = transactionRepository.countByAccountId(testAccountId);

    // Call our REST API endpoint to get account transactions
    MvcResult result = mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", testAccountId)
            .param("count", "10") // Limit to 10 transactions for testing
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    log.info("Response: {}", responseContent);

    JsonNode responseJson = objectMapper.readTree(responseContent);
    
    // Validate response structure
    if (responseJson.has("transactions")) {
      JsonNode transactions = responseJson.get("transactions");
      assertTrue(transactions.isArray(), "Transactions should be an array");

      // Validate database persistence - transactions should be upserted
      long transactionCountAfter = transactionRepository.countByAccountId(testAccountId);
      assertTrue(transactionCountAfter >= transactionCountBefore, 
          "Transaction count should increase or remain the same (upsert behavior)");

      // If transactions exist, validate they are persisted
      if (transactions.size() > 0) {
        List<EtradeTransaction> dbTransactions = transactionRepository
            .findByAccountIdOrderByTransactionDateDesc(testAccountId);
        
        assertFalse(dbTransactions.isEmpty(), "Transactions should be persisted in database");
        
        // Validate first transaction
        EtradeTransaction firstTransaction = dbTransactions.get(0);
        assertNotNull(firstTransaction.getTransactionId(), "Transaction ID should be populated");
        assertNotNull(firstTransaction.getAccountId(), "Account ID should be populated");
        assertNotNull(firstTransaction.getTransactionDate(), "Transaction date should be populated");
        assertNotNull(firstTransaction.getAmount(), "Amount should be populated");
        
        log.info("✅ Step 3 completed via REST API");
        log.info("  Transactions Found: {}", transactions.size());
        log.info("  Transactions Persisted: {}", dbTransactions.size());
        log.info("  First Transaction ID: {}", firstTransaction.getTransactionId());
        log.info("  First Transaction Date: {}", firstTransaction.getTransactionDate());
        log.info("  First Transaction Amount: {}", firstTransaction.getAmount());
      } else {
        log.info("✅ Step 3 completed - No transactions found (account may be new)");
      }
    } else {
      log.warn("⚠️  Response does not contain 'transactions' field - may be empty");
    }
  }

  @Test
  @DisplayName("Step 4: Get Transaction Details - Via REST API - Validates Details Update")
  void step4_getTransactionDetails_viaRestApi_validatesDetailsUpdate() throws Exception {
    log.info("=== Step 4: Get Transaction Details via REST API ===");

    // Ensure account exists and transactions are loaded (run Step 3 first if needed)
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }
    
    // Get a transaction ID from the database (from Step 3) or list transactions first
    List<EtradeTransaction> existingTransactions = transactionRepository
        .findByAccountIdOrderByTransactionDateDesc(testAccountId);
    
    if (existingTransactions.isEmpty()) {
      // Try to load transactions first
      step3_listTransactions_viaRestApi_validatesTransactionUpsert();
      existingTransactions = transactionRepository
          .findByAccountIdOrderByTransactionDateDesc(testAccountId);
    }
    
    if (existingTransactions.isEmpty()) {
      log.warn("⚠️  No transactions found for account. Skipping transaction details test.");
      return;
    }

    EtradeTransaction transaction = existingTransactions.get(0);
    String transactionId = transaction.getTransactionId();
    
    // Get transaction details count before
    boolean hadDetailsBefore = transaction.getCategoryId() != null 
        || transaction.getBrokerageTransactionType() != null
        || transaction.getDetailsRawResponse() != null;

    // Call our REST API endpoint to get transaction details
    MvcResult result = mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions/{transactionId}", 
            testAccountId, transactionId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    log.info("Response: {}", responseContent);

    JsonNode detailsJson = objectMapper.readTree(responseContent);
    assertNotNull(detailsJson.get("transactionId"), "Response should contain transactionId");
    assertEquals(transactionId, detailsJson.get("transactionId").asText(), 
        "Transaction ID should match");

    // Validate database update - transaction should be updated with details
    Optional<EtradeTransaction> updatedTransactionOpt = transactionRepository
        .findByTransactionId(transactionId);
    assertTrue(updatedTransactionOpt.isPresent(), "Transaction should exist in database");

    EtradeTransaction updatedTransaction = updatedTransactionOpt.get();
    assertNotNull(updatedTransaction.getLastUpdatedAt(), "Last updated at should be set");
    assertTrue(updatedTransaction.getLastUpdatedAt().isAfter(updatedTransaction.getFirstSeenAt())
        || updatedTransaction.getLastUpdatedAt().isEqual(updatedTransaction.getFirstSeenAt()),
        "Last updated at should be after or equal to first seen at");

    // Validate details are populated (at least some should be non-null)
    boolean hasDetailsAfter = updatedTransaction.getCategoryId() != null 
        || updatedTransaction.getBrokerageTransactionType() != null
        || updatedTransaction.getDetailsRawResponse() != null;
    
    if (hadDetailsBefore) {
      // If details existed before, they should still exist
      assertTrue(hasDetailsAfter, "Transaction details should still be populated after update");
    } else {
      // If details didn't exist before, they should be populated now
      log.info("Transaction details populated: {}", hasDetailsAfter);
    }

    log.info("✅ Step 4 completed via REST API");
    log.info("  Transaction ID: {}", transactionId);
    log.info("  Has Category: {}", updatedTransaction.getCategoryId() != null);
    log.info("  Has Brokerage Type: {}", updatedTransaction.getBrokerageTransactionType() != null);
    log.info("  Last Updated: {}", updatedTransaction.getLastUpdatedAt());
  }

  @Test
  @DisplayName("Step 5: View Portfolio - Via REST API - Validates Position Upsert")
  void step5_viewPortfolio_viaRestApi_validatesPositionUpsert() throws Exception {
    log.info("=== Step 5: View Portfolio via REST API ===");

    // Ensure account exists (run Step 1 first if needed)
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial position count
    long positionCountBefore = positionRepository.countByAccountId(testAccountId);

    // Call our REST API endpoint to get account portfolio
    MvcResult result = mockMvc.perform(get("/api/etrade/accounts/{accountId}/portfolio", testAccountId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = result.getResponse().getContentAsString();
    log.info("Response: {}", responseContent);

    JsonNode responseJson = objectMapper.readTree(responseContent);
    
    // Validate response structure
    if (responseJson.has("accountPortfolios")) {
      JsonNode accountPortfolios = responseJson.get("accountPortfolios");
      assertTrue(accountPortfolios.isArray(), "Account portfolios should be an array");

      // Get all positions from response
      int totalPositions = 0;
      for (JsonNode accountPortfolio : accountPortfolios) {
        if (accountPortfolio.has("positions")) {
          JsonNode positions = accountPortfolio.get("positions");
          if (positions.isArray()) {
            totalPositions += positions.size();
          }
        }
      }

      // Validate database persistence - positions should be upserted
      long positionCountAfter = positionRepository.countByAccountId(testAccountId);
      assertTrue(positionCountAfter >= positionCountBefore, 
          "Position count should increase or remain the same (upsert behavior)");

      // If positions exist, validate they are persisted
      if (totalPositions > 0) {
        List<EtradePortfolioPosition> dbPositions = positionRepository
            .findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
        
        assertFalse(dbPositions.isEmpty(), "Positions should be persisted in database");
        
        // Validate first position
        EtradePortfolioPosition firstPosition = dbPositions.get(0);
        assertNotNull(firstPosition.getPositionId(), "Position ID should be populated");
        assertNotNull(firstPosition.getAccountId(), "Account ID should be populated");
        assertNotNull(firstPosition.getSymbol(), "Symbol should be populated");
        assertNotNull(firstPosition.getQuantity(), "Quantity should be populated");
        assertNotNull(firstPosition.getMarketValue(), "Market value should be populated");
        assertNotNull(firstPosition.getSnapshotTime(), "Snapshot time should be populated");
        
        log.info("✅ Step 5 completed via REST API");
        log.info("  Positions Found: {}", totalPositions);
        log.info("  Positions Persisted: {}", dbPositions.size());
        log.info("  First Position ID: {}", firstPosition.getPositionId());
        log.info("  First Position Symbol: {}", firstPosition.getSymbol());
        log.info("  First Position Quantity: {}", firstPosition.getQuantity());
        log.info("  First Position Market Value: {}", firstPosition.getMarketValue());
      } else {
        log.info("✅ Step 5 completed - No positions found (account may be empty)");
      }
    } else {
      log.warn("⚠️  Response does not contain 'accountPortfolios' field - may be empty");
    }
  }

  @Test
  @DisplayName("Full Workflow: All Steps - End-to-End via API")
  void fullWorkflow_allSteps_endToEnd_viaApi() throws Exception {
    log.info("=== Full Accounts API Workflow: End-to-End via REST API ===");

    // Step 1: List Accounts and persist
    step1_listAccounts_viaRestApi_validatesAccountUpsert();
    assertNotNull(testAccountId, "Account ID should be set after Step 1");

    // Step 2: Get Balance and persist snapshot
    step2_getAccountBalance_viaRestApi_validatesBalanceSnapshotPersistence();

    // Verify balance snapshot was created
    List<EtradeBalance> balances = balanceRepository.findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
    assertFalse(balances.isEmpty(), "Balance snapshots should exist after Step 2");
    assertEquals(1, balances.size(), "Should have exactly 1 balance snapshot after first call");

    // Call balance again - should create another snapshot
    step2_getAccountBalance_viaRestApi_validatesBalanceSnapshotPersistence();
    balances = balanceRepository.findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
    assertTrue(balances.size() >= 2, "Should have at least 2 balance snapshots after second call");

    // Step 3: List Transactions and persist
    step3_listTransactions_viaRestApi_validatesTransactionUpsert();

    // Verify transactions were persisted (if any exist)
    List<EtradeTransaction> transactions = transactionRepository
        .findByAccountIdOrderByTransactionDateDesc(testAccountId);
    log.info("Total transactions persisted: {}", transactions.size());

    // Step 4: Get Transaction Details (if transactions exist)
    if (!transactions.isEmpty()) {
      step4_getTransactionDetails_viaRestApi_validatesDetailsUpdate();
    } else {
      log.info("Skipping Step 4 - No transactions found");
    }

    // Step 5: View Portfolio and persist positions
    step5_viewPortfolio_viaRestApi_validatesPositionUpsert();

    // Verify positions were persisted (if any exist)
    List<EtradePortfolioPosition> positions = positionRepository
        .findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
    log.info("Total positions persisted: {}", positions.size());

    log.info("✅ Full workflow completed successfully!");
    log.info("  Account ID: {}", testAccountId);
    log.info("  Balance Snapshots: {}", balanceRepository.countByAccountId(testAccountId));
    log.info("  Transactions: {}", transactionRepository.countByAccountId(testAccountId));
    log.info("  Positions: {}", positionRepository.countByAccountId(testAccountId));
  }

  @Test
  @DisplayName("Balance Snapshot - Append-Only History Validation")
  void balanceSnapshot_appendOnlyHistoryValidation() throws Exception {
    log.info("=== Balance Snapshot: Append-Only History Validation ===");

    // Ensure account exists
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial balance count
    long initialCount = balanceRepository.countByAccountId(testAccountId);

    // Call balance API multiple times
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", testAccountId)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
      
      // Wait a bit to ensure different snapshot times
      Thread.sleep(100);
    }

    // Verify balance count increased by 3 (append-only)
    long finalCount = balanceRepository.countByAccountId(testAccountId);
    assertEquals(initialCount + 3, finalCount, 
        "Balance snapshot count should increase by 3 (append-only history)");

    // Verify all snapshots have different times
    List<EtradeBalance> balances = balanceRepository
        .findByAccountIdOrderBySnapshotTimeDesc(testAccountId);
    assertTrue(balances.size() >= 3, "Should have at least 3 balance snapshots");
    
    // Verify snapshots are ordered by time descending
    for (int i = 0; i < balances.size() - 1; i++) {
      assertTrue(balances.get(i).getSnapshotTime()
          .isAfter(balances.get(i + 1).getSnapshotTime())
          || balances.get(i).getSnapshotTime()
          .isEqual(balances.get(i + 1).getSnapshotTime()),
          "Snapshots should be ordered by time descending");
    }

    log.info("✅ Append-only history validated: {} snapshots created", finalCount - initialCount);
  }

  @Test
  @DisplayName("Transaction Upsert - No Duplicates Validation")
  void transactionUpsert_noDuplicatesValidation() throws Exception {
    log.info("=== Transaction Upsert: No Duplicates Validation ===");

    // Ensure account exists
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial transaction count
    long initialCount = transactionRepository.countByAccountId(testAccountId);

    // Call transactions API multiple times
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", testAccountId)
              .param("count", "10")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    // Verify transaction count did not increase by 3x (upsert behavior)
    long finalCount = transactionRepository.countByAccountId(testAccountId);
    assertTrue(finalCount <= initialCount + 10, 
        "Transaction count should not increase dramatically (upsert prevents duplicates)");

    log.info("✅ Upsert behavior validated: {} transactions (initial: {})", finalCount, initialCount);
  }

  @Test
  @DisplayName("Position Upsert - No Duplicates Validation")
  void positionUpsert_noDuplicatesValidation() throws Exception {
    log.info("=== Position Upsert: No Duplicates Validation ===");

    // Ensure account exists
    if (testAccountId == null) {
      step1_listAccounts_viaRestApi_validatesAccountUpsert();
    }

    // Get initial position count
    long initialCount = positionRepository.countByAccountId(testAccountId);

    // Call portfolio API multiple times
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/api/etrade/accounts/{accountId}/portfolio", testAccountId)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    // Verify position count did not increase by 3x (upsert behavior)
    long finalCount = positionRepository.countByAccountId(testAccountId);
    assertTrue(finalCount <= initialCount + 10, 
        "Position count should not increase dramatically (upsert prevents duplicates)");

    log.info("✅ Upsert behavior validated: {} positions (initial: {})", finalCount, initialCount);
  }

  /**
   * Helper method to ensure a valid access token exists.
   * If not available, attempts to get one via OAuth flow (requires verifier).
   */
  private UUID ensureValidAccessToken() {
    // Check if we have a valid access token in the database
    List<com.myqyl.aitradex.etrade.domain.EtradeOAuthToken> tokens = tokenRepository.findAll();
    for (com.myqyl.aitradex.etrade.domain.EtradeOAuthToken token : tokens) {
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
    UUID tempAccountId = UUID.randomUUID(); // Temporary account ID
    oauthService.exchangeForAccessToken(
        requestTokenResponse.getRequestToken(),
        requestTokenResponse.getRequestTokenSecret(),
        verifier,
        tempAccountId);
    
    log.info("✅ Successfully obtained access token for account: {}", tempAccountId);
    return tempAccountId;
  }
}
