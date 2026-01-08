package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myqyl.aitradex.etrade.api.integration.EtradeApiIntegrationTestBase;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for E*TRADE Accounts API endpoints.
 * 
 * These tests validate our application's account functionality by:
 * - Calling our REST API endpoints (/api/etrade/accounts/*)
 * - Mocking the underlying E*TRADE client calls
 * - Validating request building, response parsing, error handling
 * 
 * Tests do NOT call E*TRADE's public endpoints directly.
 */
@DisplayName("E*TRADE Accounts API Integration Tests")
class EtradeAccountsApiIntegrationTest extends EtradeApiIntegrationTestBase {

  // ============================================================================
  // 1. LIST ACCOUNTS TESTS
  // ============================================================================

  @Test
  @DisplayName("Get User Accounts - Success")
  void getUserAccounts_success() throws Exception {
    // Call our application endpoint (uses database, no E*TRADE client call needed)
    mockMvc.perform(get("/api/etrade/accounts")
            .param("userId", testUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].accountIdKey").value(testAccountIdKey))
        .andExpect(jsonPath("$[0].accountName").value("Test Account"));

    // No E*TRADE client call for this endpoint (uses database)
    verify(accountClient, never()).getAccountList(any());
  }

  @Test
  @DisplayName("Get Account - Success")
  void getAccount_success() throws Exception {
    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/accounts/{accountId}", testAccountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testAccountId.toString()))
        .andExpect(jsonPath("$.accountIdKey").value(testAccountIdKey))
        .andExpect(jsonPath("$.accountName").value("Test Account"));

    // No E*TRADE client call for this endpoint (uses database)
    verify(accountClient, never()).getAccountList(any());
  }

  @Test
  @DisplayName("Get Account - Not Found")
  void getAccount_notFound() throws Exception {
    UUID nonExistentAccountId = UUID.randomUUID();

    mockMvc.perform(get("/api/etrade/accounts/{accountId}", nonExistentAccountId))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    verify(accountClient, never()).getAccountList(any());
  }

  // ============================================================================
  // 2. ACCOUNT BALANCE TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Account Balance - Success")
  void getAccountBalance_success() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockBalance = createMockBalance();
    when(accountClient.getBalance(eq(testAccountId), eq(testAccountIdKey)))
        .thenReturn(mockBalance);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", testAccountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").exists())
        .andExpect(jsonPath("$.accountType").value("BROKERAGE"))
        .andExpect(jsonPath("$.computed").exists());

    verify(accountClient, times(1)).getBalance(eq(testAccountId), eq(testAccountIdKey));
  }

  @Test
  @DisplayName("Get Account Balance - Invalid Account")
  void getAccountBalance_invalidAccount() throws Exception {
    UUID invalidAccountId = UUID.randomUUID();

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", invalidAccountId))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    verify(accountClient, never()).getBalance(any(), any());
  }

  // ============================================================================
  // 3. ACCOUNT PORTFOLIO TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Account Portfolio - Success")
  void getAccountPortfolio_success() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockPortfolio = createMockPortfolio();
    when(accountClient.getPortfolio(eq(testAccountId), eq(testAccountIdKey)))
        .thenReturn(mockPortfolio);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/accounts/{accountId}/portfolio", testAccountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").exists())
        .andExpect(jsonPath("$.positions").isArray())
        .andExpect(jsonPath("$.positions[0].symbol").value("AAPL"));

    verify(accountClient, times(1)).getPortfolio(eq(testAccountId), eq(testAccountIdKey));
  }

  @Test
  @DisplayName("Get Account Portfolio - Invalid Account")
  void getAccountPortfolio_invalidAccount() throws Exception {
    UUID invalidAccountId = UUID.randomUUID();

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/portfolio", invalidAccountId))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    verify(accountClient, never()).getPortfolio(any(), any());
  }

  // ============================================================================
  // 4. SYNC ACCOUNTS TESTS
  // ============================================================================

  @Test
  @DisplayName("Sync Accounts - Success")
  void syncAccounts_success() throws Exception {
    // Mock E*TRADE client response
    List<Map<String, Object>> mockAccounts = List.of(
        createMockAccountData("12345678", "BROKERAGE", "Test Account", "ACTIVE")
    );
    when(accountClient.getAccountList(eq(testAccountId)))
        .thenReturn(mockAccounts);

    // Call our application endpoint
    mockMvc.perform(post("/api/etrade/accounts/sync")
            .param("userId", testUserId.toString())
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    verify(accountClient, times(1)).getAccountList(eq(testAccountId));
  }

  // ============================================================================
  // 5. UNLINK ACCOUNT TESTS
  // ============================================================================

  @Test
  @DisplayName("Unlink Account - Success")
  void unlinkAccount_success() throws Exception {
    // Call our application endpoint
    mockMvc.perform(delete("/api/etrade/accounts/{accountId}", testAccountId))
        .andExpect(status().isNoContent());

    // Verify account was deleted from database
    mockMvc.perform(get("/api/etrade/accounts/{accountId}", testAccountId))
        .andExpect(status().isInternalServerError()); // Should not exist anymore
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private Map<String, Object> createMockBalance() {
    Map<String, Object> balance = new HashMap<>();
    balance.put("accountId", testAccountIdKey);
    balance.put("accountType", "BROKERAGE");
    
    Map<String, Object> computed = new HashMap<>();
    computed.put("total", "100000.00");
    computed.put("netCash", "50000.00");
    computed.put("cashAvailableForInvestment", "45000.00");
    balance.put("computed", computed);
    
    return balance;
  }

  private Map<String, Object> createMockPortfolio() {
    Map<String, Object> portfolio = new HashMap<>();
    portfolio.put("accountId", testAccountIdKey);
    
    List<Map<String, Object>> positions = new ArrayList<>();
    Map<String, Object> position = new HashMap<>();
    position.put("symbol", "AAPL");
    position.put("quantity", 10.0);
    position.put("marketValue", 1518.00);
    position.put("lastTrade", 151.80);
    positions.add(position);
    
    portfolio.put("positions", positions);
    return portfolio;
  }

  private Map<String, Object> createMockAccountData(String accountIdKey, String accountType, 
                                                     String accountName, String accountStatus) {
    Map<String, Object> account = new HashMap<>();
    account.put("accountIdKey", accountIdKey);
    account.put("accountId", accountIdKey);
    account.put("accountType", accountType);
    account.put("accountName", accountName);
    account.put("accountStatus", accountStatus);
    account.put("accountDesc", accountName);
    return account;
  }
}
