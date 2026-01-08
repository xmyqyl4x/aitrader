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
 * Integration tests for E*TRADE Transactions API endpoints.
 * 
 * These tests validate our application's transaction functionality by:
 * - Calling our REST API endpoints (/api/etrade/accounts/{accountId}/transactions/*)
 * - Mocking the underlying E*TRADE client calls
 * - Validating request building, response parsing, error handling
 * 
 * Tests do NOT call E*TRADE's public endpoints directly.
 */
@DisplayName("E*TRADE Transactions API Integration Tests")
class EtradeTransactionsApiIntegrationTest extends EtradeApiIntegrationTestBase {

  // ============================================================================
  // 1. LIST TRANSACTIONS TESTS
  // ============================================================================

  @Test
  @DisplayName("List Transactions - Success")
  void listTransactions_success() throws Exception {
    // Mock E*TRADE client response with metadata
    Map<String, Object> mockResponse = createMockTransactionResponse();
    when(accountClient.getTransactions(eq(testAccountId), eq(testAccountIdKey), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(mockResponse);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", testAccountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionCount").value(1))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.moreTransactions").value(false))
        .andExpect(jsonPath("$.transactions").isArray())
        .andExpect(jsonPath("$.transactions[0].transactionId").value("TXN123"))
        .andExpect(jsonPath("$.transactions[0].amount").value("1000.00"))
        .andExpect(jsonPath("$.transactions[0].transactionType").value("BUY"));

    verify(accountClient, times(1)).getTransactions(eq(testAccountId), eq(testAccountIdKey), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("List Transactions - With Pagination")
  void listTransactions_withPagination() throws Exception {
    Map<String, Object> mockResponse = createMockTransactionResponseWithPagination();
    when(accountClient.getTransactions(eq(testAccountId), eq(testAccountIdKey), isNull(), eq(3), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(mockResponse);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", testAccountId)
            .param("count", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionCount").value(3))
        .andExpect(jsonPath("$.moreTransactions").value(true))
        .andExpect(jsonPath("$.transactions").isArray())
        .andExpect(jsonPath("$.transactions").value(org.hamcrest.Matchers.hasSize(3)));

    verify(accountClient, times(1)).getTransactions(eq(testAccountId), eq(testAccountIdKey), isNull(), eq(3), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("List Transactions - With Parameters")
  void listTransactions_withParameters() throws Exception {
    Map<String, Object> mockResponse = createMockTransactionResponse();
    when(accountClient.getTransactions(eq(testAccountId), eq(testAccountIdKey), eq("MARKER123"), eq(10), 
                                       eq("01012024"), eq("12312024"), eq("DESC"), eq("json"), eq("STORE123")))
        .thenReturn(mockResponse);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", testAccountId)
            .param("marker", "MARKER123")
            .param("count", "10")
            .param("startDate", "01012024")
            .param("endDate", "12312024")
            .param("sortOrder", "DESC")
            .param("accept", "json")
            .param("storeId", "STORE123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactions").isArray());

    verify(accountClient, times(1)).getTransactions(eq(testAccountId), eq(testAccountIdKey), eq("MARKER123"), eq(10), 
                                                    eq("01012024"), eq("12312024"), eq("DESC"), eq("json"), eq("STORE123"));
  }

  @Test
  @DisplayName("List Transactions - Invalid Account")
  void listTransactions_invalidAccount() throws Exception {
    UUID invalidAccountId = UUID.randomUUID();

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions", invalidAccountId))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    verify(accountClient, never()).getTransactions(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  // ============================================================================
  // 2. GET TRANSACTION DETAILS TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Transaction Details - Success")
  void getTransactionDetails_success() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockDetails = createMockTransactionDetails("TXN123", "2024-01-15", 1000.00, "BUY");
    when(accountClient.getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("TXN123"), isNull(), isNull()))
        .thenReturn(mockDetails);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions/{transactionId}", 
            testAccountId, "TXN123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("TXN123"))
        .andExpect(jsonPath("$.amount").value("1000.00"))
        .andExpect(jsonPath("$.description").exists());

    verify(accountClient, times(1)).getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("TXN123"), isNull(), isNull());
  }

  @Test
  @DisplayName("Get Transaction Details - With Parameters")
  void getTransactionDetails_withParameters() throws Exception {
    Map<String, Object> mockDetails = createMockTransactionDetails("TXN123", "2024-01-15", 1000.00, "BUY");
    when(accountClient.getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("TXN123"), eq("json"), eq("STORE123")))
        .thenReturn(mockDetails);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions/{transactionId}", 
            testAccountId, "TXN123")
            .param("accept", "json")
            .param("storeId", "STORE123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("TXN123"));

    verify(accountClient, times(1)).getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("TXN123"), eq("json"), eq("STORE123"));
  }

  @Test
  @DisplayName("Get Transaction Details - Invalid Transaction ID")
  void getTransactionDetails_invalidTransactionId() throws Exception {
    when(accountClient.getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("INVALID"), isNull(), isNull()))
        .thenThrow(new RuntimeException("Transaction not found"));

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/transactions/{transactionId}", 
            testAccountId, "INVALID"))
        .andExpect(status().isInternalServerError());

    verify(accountClient, times(1)).getTransactionDetails(eq(testAccountId), eq(testAccountIdKey), eq("INVALID"), isNull(), isNull());
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private Map<String, Object> createMockTransactionResponse() {
    Map<String, Object> response = new HashMap<>();
    response.put("transactionCount", 1);
    response.put("totalCount", 1);
    response.put("moreTransactions", false);
    
    List<Map<String, Object>> transactions = List.of(
        createMockTransaction("TXN123", "2024-01-15", 1000.00, "BUY", "AAPL")
    );
    response.put("transactions", transactions);
    
    return response;
  }

  private Map<String, Object> createMockTransactionResponseWithPagination() {
    Map<String, Object> response = new HashMap<>();
    response.put("transactionCount", 3);
    response.put("totalCount", 10);
    response.put("moreTransactions", true);
    response.put("next", "NEXT_MARKER");
    response.put("marker", "CURRENT_MARKER");
    
    List<Map<String, Object>> transactions = List.of(
        createMockTransaction("TXN1", "2024-01-15", 1000.00, "BUY", "AAPL"),
        createMockTransaction("TXN2", "2024-01-14", 500.00, "SELL", "MSFT"),
        createMockTransaction("TXN3", "2024-01-13", 750.00, "BUY", "GOOGL")
    );
    response.put("transactions", transactions);
    
    return response;
  }

  private Map<String, Object> createMockTransaction(String transactionId, String date, 
                                                     double amount, String type, String symbol) {
    Map<String, Object> transaction = new HashMap<>();
    transaction.put("transactionId", transactionId);
    transaction.put("accountId", testAccountIdKey);
    transaction.put("transactionDate", date);
    transaction.put("amount", String.valueOf(amount));
    transaction.put("description", type + " " + symbol);
    transaction.put("transactionType", type);
    transaction.put("instType", "EQ");
    transaction.put("detailsURI", "https://api.etrade.com/v1/accounts/" + testAccountIdKey + "/transactions/" + transactionId);
    return transaction;
  }

  private Map<String, Object> createMockTransactionDetails(String transactionId, String date, 
                                                            double amount, String type) {
    Map<String, Object> details = new HashMap<>();
    details.put("transactionId", transactionId);
    details.put("accountId", testAccountIdKey);
    details.put("transactionDate", date);
    details.put("amount", String.valueOf(amount));
    details.put("description", type + " transaction");
    
    Map<String, Object> category = new HashMap<>();
    category.put("categoryId", "CAT123");
    category.put("parentId", "PARENT123");
    details.put("category", category);
    
    Map<String, Object> brokerage = new HashMap<>();
    brokerage.put("transactionType", type);
    details.put("brokerage", brokerage);
    
    return details;
  }
}
