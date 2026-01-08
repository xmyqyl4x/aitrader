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
 * Integration tests for E*TRADE Quotes/Market API endpoints.
 * 
 * These tests validate our application's quote functionality by:
 * - Calling our REST API endpoints (/api/etrade/quotes/*)
 * - Mocking the underlying E*TRADE client calls
 * - Validating request building, response parsing, error handling
 * 
 * Tests do NOT call E*TRADE's public endpoints directly.
 */
@DisplayName("E*TRADE Quotes/Market API Integration Tests")
class EtradeQuotesApiIntegrationTest extends EtradeApiIntegrationTestBase {

  // ============================================================================
  // 1. GET QUOTES TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Quote - Single Symbol")
  void getQuote_singleSymbol() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockQuote = createMockQuote("AAPL", 151.80, "Apple Inc.");
    when(quoteClient.getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(mockQuote));

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes/{symbol}", "AAPL")
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.symbol").value("AAPL"))
        .andExpect(jsonPath("$.lastTrade").value(151.80))
        .andExpect(jsonPath("$.companyName").value("Apple Inc."));

    // Verify our service called the E*TRADE client
    verify(quoteClient, times(1)).getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("Get Quote - With Parameters")
  void getQuote_withParameters() throws Exception {
    Map<String, Object> mockQuote = createMockQuote("AAPL", 151.80, "Apple Inc.");
    when(quoteClient.getQuotes(eq(testAccountId), any(String[].class), eq("ALL"), eq(true), eq(100), eq(false)))
        .thenReturn(List.of(mockQuote));

    mockMvc.perform(get("/api/etrade/quotes/{symbol}", "AAPL")
            .param("accountId", testAccountId.toString())
            .param("detailFlag", "ALL")
            .param("requireEarningsDate", "true")
            .param("overrideSymbolCount", "100")
            .param("skipMiniOptionsCheck", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.symbol").value("AAPL"));

    verify(quoteClient, times(1)).getQuotes(eq(testAccountId), any(String[].class), eq("ALL"), eq(true), eq(100), eq(false));
  }

  @Test
  @DisplayName("Get Quotes - Multiple Symbols")
  void getQuotes_multipleSymbols() throws Exception {
    // Mock E*TRADE client response
    List<Map<String, Object>> mockQuotes = List.of(
        createMockQuote("AAPL", 151.80, "Apple Inc."),
        createMockQuote("MSFT", 352.50, "Microsoft Corp.")
    );
    when(quoteClient.getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(mockQuotes);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", "AAPL,MSFT")
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").value("AAPL"))
        .andExpect(jsonPath("$[1].symbol").value("MSFT"));

    // Verify our service called the E*TRADE client
    verify(quoteClient, times(1)).getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("Get Quote - Invalid Symbol")
  void getQuote_invalidSymbol() throws Exception {
    // Mock E*TRADE client to return empty list
    when(quoteClient.getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(Collections.emptyList());

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes/{symbol}", "INVALID")
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    verify(quoteClient, times(1)).getQuotes(eq(testAccountId), any(String[].class), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("Get Quote - Invalid Account")
  void getQuote_invalidAccount() throws Exception {
    UUID invalidAccountId = UUID.randomUUID();

    // Call our application endpoint with invalid account
    mockMvc.perform(get("/api/etrade/quotes/{symbol}", "AAPL")
            .param("accountId", invalidAccountId.toString()))
        .andExpect(status().isInternalServerError()); // Our service throws RuntimeException

    // Verify E*TRADE client was never called
    verify(quoteClient, never()).getQuotes(any(), any());
  }

  // ============================================================================
  // 2. LOOK UP PRODUCT TESTS
  // ============================================================================

  @Test
  @DisplayName("Look Up Product - By Symbol")
  void lookupProduct_bySymbol() throws Exception {
    // Mock E*TRADE client response
    List<Map<String, Object>> mockProducts = List.of(
        createMockProductLookup("AAPL", "Apple Inc.", "EQ")
    );
    when(quoteClient.lookupProduct(eq("AAPL")))
        .thenReturn(mockProducts);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes/lookup")
            .param("input", "AAPL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").value("AAPL"))
        .andExpect(jsonPath("$[0].description").value("Apple Inc."));

    verify(quoteClient, times(1)).lookupProduct(eq("AAPL"));
  }

  @Test
  @DisplayName("Look Up Product - By Company Name")
  void lookupProduct_byCompanyName() throws Exception {
    List<Map<String, Object>> mockProducts = List.of(
        createMockProductLookup("AAPL", "Apple Inc.", "EQ"),
        createMockProductLookup("AAPL", "Apple Inc. Common Stock", "EQ")
    );
    when(quoteClient.lookupProduct(eq("Apple")))
        .thenReturn(mockProducts);

    mockMvc.perform(get("/api/etrade/quotes/lookup")
            .param("input", "Apple"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").value("AAPL"));

    verify(quoteClient, times(1)).lookupProduct(eq("Apple"));
  }

  // ============================================================================
  // 3. OPTION CHAINS TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Option Chains - Basic")
  void getOptionChains_basic() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockChains = createMockOptionChains();
    when(quoteClient.getOptionChains(eq("AAPL"), eq(2024), eq(1), eq(19),
        eq(150), eq(5), isNull(), isNull()))
        .thenReturn(mockChains);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", "AAPL")
            .param("expiryYear", "2024")
            .param("expiryMonth", "1")
            .param("expiryDay", "19")
            .param("strikePriceNear", "150")
            .param("noOfStrikes", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.optionPairs").exists());

    verify(quoteClient, times(1)).getOptionChains(eq("AAPL"), eq(2024), eq(1), eq(19),
        eq(150), eq(5), isNull(), isNull());
  }

  @Test
  @DisplayName("Get Option Chains - With Call/Put Filter")
  void getOptionChains_withCallPutFilter() throws Exception {
    Map<String, Object> mockChains = createMockOptionChains();
    when(quoteClient.getOptionChains(eq("AAPL"), eq(2024), eq(1), eq(19),
        eq(150), eq(5), eq("STANDARD"), eq("CALL")))
        .thenReturn(mockChains);

    mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", "AAPL")
            .param("expiryYear", "2024")
            .param("expiryMonth", "1")
            .param("expiryDay", "19")
            .param("strikePriceNear", "150")
            .param("noOfStrikes", "5")
            .param("optionCategory", "STANDARD")
            .param("chainType", "CALL"))
        .andExpect(status().isOk());

    verify(quoteClient, times(1)).getOptionChains(eq("AAPL"), eq(2024), eq(1), eq(19),
        eq(150), eq(5), eq("STANDARD"), eq("CALL"));
  }

  // ============================================================================
  // 4. OPTION EXPIRE DATES TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Option Expire Dates - Basic")
  void getOptionExpireDates_basic() throws Exception {
    // Mock E*TRADE client response
    List<Map<String, Object>> mockDates = List.of(
        createMockExpireDate(2024, 1, 19),
        createMockExpireDate(2024, 2, 16)
    );
    when(quoteClient.getOptionExpireDates(eq("AAPL"), isNull()))
        .thenReturn(mockDates);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/quotes/option-expire-dates")
            .param("symbol", "AAPL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].year").value(2024))
        .andExpect(jsonPath("$[0].month").value(1))
        .andExpect(jsonPath("$[0].day").value(19));

    verify(quoteClient, times(1)).getOptionExpireDates(eq("AAPL"), isNull());
  }

  @Test
  @DisplayName("Get Option Expire Dates - With Expiry Type")
  void getOptionExpireDates_withExpiryType() throws Exception {
    List<Map<String, Object>> mockDates = List.of(
        createMockExpireDate(2024, 1, 19)
    );
    when(quoteClient.getOptionExpireDates(eq("AAPL"), eq("WEEKLY")))
        .thenReturn(mockDates);

    mockMvc.perform(get("/api/etrade/quotes/option-expire-dates")
            .param("symbol", "AAPL")
            .param("expiryType", "WEEKLY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    verify(quoteClient, times(1)).getOptionExpireDates(eq("AAPL"), eq("WEEKLY"));
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private Map<String, Object> createMockQuote(String symbol, double lastTrade, String companyName) {
    Map<String, Object> quote = new HashMap<>();
    quote.put("symbol", symbol);
    quote.put("lastTrade", lastTrade);
    quote.put("companyName", companyName);
    quote.put("previousClose", lastTrade - 1.0);
    quote.put("open", lastTrade - 0.5);
    quote.put("high", lastTrade + 1.0);
    quote.put("low", lastTrade - 1.0);
    quote.put("volume", 50000000L);
    quote.put("change", 1.80);
    quote.put("changePercent", 1.20);
    return quote;
  }

  private Map<String, Object> createMockProductLookup(String symbol, String description, String securityType) {
    Map<String, Object> product = new HashMap<>();
    product.put("symbol", symbol);
    product.put("description", description);
    product.put("securityType", securityType);
    return product;
  }

  private Map<String, Object> createMockOptionChains() {
    Map<String, Object> chain = new HashMap<>();
    List<Map<String, Object>> pairs = new ArrayList<>();
    
    Map<String, Object> pair = new HashMap<>();
    Map<String, Object> call = new HashMap<>();
    call.put("strikePrice", 150.0);
    call.put("bid", 2.50);
    call.put("ask", 2.60);
    call.put("lastPrice", 2.55);
    call.put("volume", 1000L);
    pair.put("call", call);
    pairs.add(pair);
    
    chain.put("optionPairs", pairs);
    return chain;
  }

  private Map<String, Object> createMockExpireDate(int year, int month, int day) {
    Map<String, Object> date = new HashMap<>();
    date.put("year", year);
    date.put("month", month);
    date.put("day", day);
    return date;
  }
}
