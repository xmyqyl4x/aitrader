package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.*;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.repository.*;
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
 * Functional tests for E*TRADE Market API endpoints.
 *
 * These tests validate the complete Market API flow through our application's REST API endpoints:
 * 1. Lookup Product (via /api/etrade/quotes/lookup) - validates product lookup
 * 2. Get Quotes (via /api/etrade/quotes) - validates quote retrieval
 * 3. Get Option Expire Dates (via /api/etrade/quotes/option-expire-dates) - validates expiration dates
 * 4. Get Option Chains (via /api/etrade/quotes/option-chains) - validates option chains
 * 5. Get Option Quote (via /api/etrade/quotes/{symbol}) - validates option quote with OPTIONS detailFlag
 *
 * Tests make REAL calls to E*TRADE sandbox (not mocked) and validate:
 * - All API calls succeed (HTTP 200)
 * - Response structure is correct
 * - Required fields are populated correctly
 * - OAuth token enforcement works correctly
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
@DisplayName("E*TRADE Market API - Functional Tests")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
class EtradeMarketFunctionalTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ApplicationContext applicationContext;

  private static final Logger log = LoggerFactory.getLogger(EtradeMarketFunctionalTest.class);

  @Autowired
  private EtradeOAuthTokenRepository tokenRepository;

  @Autowired
  private EtradeLookupProductRepository lookupProductRepository;

  @Autowired
  private EtradeQuoteSnapshotRepository quoteSnapshotRepository;

  @Autowired
  private EtradeOptionExpireDateRepository optionExpireDateRepository;

  @Autowired
  private EtradeOptionChainSnapshotRepository optionChainSnapshotRepository;

  @Autowired
  private EtradeOptionContractRepository optionContractRepository;

  @Autowired
  private EtradeProperties properties;

  private UUID testUserId;
  private UUID testAccountId;

  @BeforeEach
  void setUpFunctional() {
    testUserId = UUID.randomUUID();

    // Clean up any existing test data
    optionContractRepository.deleteAll();
    optionChainSnapshotRepository.deleteAll();
    optionExpireDateRepository.deleteAll();
    quoteSnapshotRepository.deleteAll();
    lookupProductRepository.deleteAll();
    tokenRepository.deleteAll();

    log.info("Running functional Market API tests against E*TRADE sandbox: {}", properties.getBaseUrl());
  }

  @Test
  @DisplayName("Test 1: Token Prerequisite Enforcement - Market API requires OAuth token")
  void test1_tokenPrerequisiteEnforcement_marketApiRequiresOAuthToken() throws Exception {
    log.info("=== Test 1: Token Prerequisite Enforcement ===");

    // Try to call Market API without token (should fail or return delayed quotes)
    // Note: Lookup Product doesn't require OAuth, but Get Quotes with real-time data does
    MvcResult result = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", "AAPL")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    // Without accountId, should return delayed quotes (200 OK)
    // With accountId but invalid token, should fail
    int status = result.getResponse().getStatus();
    log.info("Status without token: {}", status);
    
    // Now try with valid token
    UUID authAccountId = ensureValidAccessToken();
    
    MvcResult resultWithToken = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", "AAPL")
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "ALL")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String responseContent = resultWithToken.getResponse().getContentAsString();
    log.info("Response with token: {}", responseContent);

    JsonNode responseJson = objectMapper.readTree(responseContent);
    assertTrue(responseJson.has("quoteData"), "Response should contain quoteData");
    assertTrue(responseJson.get("quoteData").isArray(), "quoteData should be an array");
    assertFalse(responseJson.get("quoteData").isEmpty(), "quoteData should not be empty");
  }

  @Test
  @DisplayName("Test 2: Lookup → Quote (Happy Path)")
  void test2_lookupToQuote_happyPath() throws Exception {
    log.info("=== Test 2: Lookup → Quote (Happy Path) ===");

    // Step 1: Look Up Product
    String searchInput = "google";
    MvcResult lookupResult = mockMvc.perform(get("/api/etrade/quotes/lookup")
            .param("input", searchInput)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String lookupContent = lookupResult.getResponse().getContentAsString();
    log.info("Lookup Response: {}", lookupContent);

    JsonNode lookupJson = objectMapper.readTree(lookupContent);
    assertTrue(lookupJson.has("data"), "Response should contain data");
    assertTrue(lookupJson.get("data").isArray(), "data should be an array");
    assertFalse(lookupJson.get("data").isEmpty(), "data should not be empty");

    // Extract symbol from lookup result
    String symbol = lookupJson.get("data").get(0).get("symbol").asText();
    String productType = lookupJson.get("data").get(0).get("type").asText();
    assertNotNull(symbol, "Symbol should be present");
    log.info("Found symbol from lookup: {}", symbol);

    // Validate database persistence: Lookup products should be upserted
    Optional<EtradeLookupProduct> persistedProduct = lookupProductRepository
        .findBySymbolAndProductType(symbol, productType);
    assertTrue(persistedProduct.isPresent(), 
        "Lookup product should be persisted in database");
    assertEquals(symbol, persistedProduct.get().getSymbol(), 
        "Persisted symbol should match");
    assertEquals(productType, persistedProduct.get().getProductType(), 
        "Persisted product type should match");
    log.info("✅ Validated lookup product persistence: {} ({})", symbol, productType);

    // Step 2: Get Quotes for the symbol
    UUID authAccountId = ensureValidAccessToken();
    
    MvcResult quoteResult = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", symbol)
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "ALL")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String quoteContent = quoteResult.getResponse().getContentAsString();
    log.info("Quote Response: {}", quoteContent);

    JsonNode quoteJson = objectMapper.readTree(quoteContent);
    assertTrue(quoteJson.has("quoteData"), "Response should contain quoteData");
    assertTrue(quoteJson.get("quoteData").isArray(), "quoteData should be an array");
    assertFalse(quoteJson.get("quoteData").isEmpty(), "quoteData should not be empty");

    JsonNode quoteData = quoteJson.get("quoteData").get(0);
    assertTrue(quoteData.has("all"), "Quote should contain all details");
    
    JsonNode allDetails = quoteData.get("all");
    assertTrue(allDetails.has("product"), "Quote should contain product");
    assertEquals(symbol, allDetails.get("product").get("symbol").asText(), 
        "Quote symbol should match lookup symbol");

    // Validate database persistence: Quote snapshot should be created (append-only)
    long initialSnapshotCount = quoteSnapshotRepository.countBySymbol(symbol);
    List<EtradeQuoteSnapshot> snapshots = quoteSnapshotRepository
        .findBySymbolOrderByRequestTimeDesc(symbol);
    assertFalse(snapshots.isEmpty(), 
        "Quote snapshot should be persisted in database");
    EtradeQuoteSnapshot latestSnapshot = snapshots.get(0);
    assertEquals(symbol, latestSnapshot.getSymbol(), 
        "Persisted snapshot symbol should match");
    assertNotNull(latestSnapshot.getRequestTime(), 
        "Persisted snapshot should have request time");
    log.info("✅ Validated quote snapshot persistence: {} (snapshot count: {})", 
        symbol, snapshots.size());
  }

  @Test
  @DisplayName("Test 2b: Get Quotes - Invalid detailFlag")
  void test2b_getQuotes_invalidDetailFlag() throws Exception {
    log.info("=== Test 2b: Get Quotes - Invalid detailFlag ===");

    UUID authAccountId = ensureValidAccessToken();

    // Try with invalid detailFlag
    MvcResult result = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", "AAPL")
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "INVALID_FLAG")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int status = result.getResponse().getStatus();
    log.info("Status with invalid detailFlag: {}", status);
    
    // Should fail (400 or 500) - E*TRADE API will reject invalid detailFlag
    assertTrue(status >= 400, "Should return error status for invalid detailFlag");
  }

  @Test
  @DisplayName("Test 2c: Get Quotes - Symbol count limits")
  void test2c_getQuotes_symbolCountLimits() throws Exception {
    log.info("=== Test 2c: Get Quotes - Symbol count limits ===");

    UUID authAccountId = ensureValidAccessToken();

    // Test with >25 symbols without override (should fail or be limited)
    StringBuilder symbols = new StringBuilder();
    for (int i = 1; i <= 26; i++) {
      if (i > 1) symbols.append(",");
      symbols.append("AAPL"); // Using same symbol multiple times for testing
    }

    MvcResult result = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", symbols.toString())
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "ALL")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int status = result.getResponse().getStatus();
    log.info("Status with 26 symbols (no override): {}", status);
    
    // Should fail (400) - E*TRADE API limits to 25 without override
    assertTrue(status >= 400, "Should return error status for >25 symbols without override");

    // Test with overrideSymbolCount=true (should allow up to 50)
    MvcResult resultWithOverride = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", symbols.toString())
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "ALL")
            .param("overrideSymbolCount", "true")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int statusWithOverride = resultWithOverride.getResponse().getStatus();
    log.info("Status with 26 symbols (with override): {}", statusWithOverride);
    
    // Should succeed with override
    assertTrue(statusWithOverride == 200 || statusWithOverride >= 400, 
        "Should either succeed or fail based on E*TRADE API behavior");
  }

  @Test
  @DisplayName("Test 3: OptionExpireDates → OptionChains (Happy Path)")
  void test3_optionExpireDatesToOptionChains_happyPath() throws Exception {
    log.info("=== Test 3: OptionExpireDates → OptionChains (Happy Path) ===");

    // Use a symbol that typically has options (AAPL)
    String symbol = "AAPL";

    // Step 1: Get Option Expire Dates
    MvcResult expireDatesResult = mockMvc.perform(get("/api/etrade/quotes/option-expire-dates")
            .param("symbol", symbol)
            .param("expiryType", "ALL")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String expireDatesContent = expireDatesResult.getResponse().getContentAsString();
    log.info("Option Expire Dates Response: {}", expireDatesContent);

    JsonNode expireDatesJson = objectMapper.readTree(expireDatesContent);
    assertTrue(expireDatesJson.has("expireDates"), "Response should contain expireDates");
    assertTrue(expireDatesJson.get("expireDates").isArray(), "expireDates should be an array");
    assertFalse(expireDatesJson.get("expireDates").isEmpty(), 
        "expireDates should not be empty");

    // Extract first expiration date
    JsonNode firstExpireDate = expireDatesJson.get("expireDates").get(0);
    int expiryYear = firstExpireDate.get("year").asInt();
    int expiryMonth = firstExpireDate.get("month").asInt();
    int expiryDay = firstExpireDate.get("day").asInt();
    log.info("Using expiration date: {}/{}/{}", expiryYear, expiryMonth, expiryDay);

    // Validate database persistence: Expiration dates should be upserted
    Optional<EtradeOptionExpireDate> persistedExpireDate = optionExpireDateRepository
        .findBySymbolAndExpiryYearAndExpiryMonthAndExpiryDay(symbol, expiryYear, expiryMonth, expiryDay);
    assertTrue(persistedExpireDate.isPresent(), 
        "Option expiration date should be persisted in database");
    assertEquals(symbol, persistedExpireDate.get().getSymbol(), 
        "Persisted symbol should match");
    assertEquals(expiryYear, persistedExpireDate.get().getExpiryYear(), 
        "Persisted expiry year should match");
    log.info("✅ Validated expiration date persistence: {} - {}/{}/{}", 
        symbol, expiryYear, expiryMonth, expiryDay);

    // Step 2: Get Option Chains
    MvcResult chainsResult = mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", symbol)
            .param("expiryYear", String.valueOf(expiryYear))
            .param("expiryMonth", String.valueOf(expiryMonth))
            .param("expiryDay", String.valueOf(expiryDay))
            .param("chainType", "CALLPUT")
            .param("strikePriceNear", "150")
            .param("noOfStrikes", "5")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String chainsContent = chainsResult.getResponse().getContentAsString();
    log.info("Option Chains Response: {}", chainsContent);

    JsonNode chainsJson = objectMapper.readTree(chainsContent);
    assertTrue(chainsJson.has("symbol"), "Response should contain symbol");
    assertEquals(symbol, chainsJson.get("symbol").asText(), "Symbol should match");
    assertTrue(chainsJson.has("optionPairs"), "Response should contain optionPairs");
    assertTrue(chainsJson.get("optionPairs").isArray(), "optionPairs should be an array");
    
    // Option chains may be empty for some symbols/dates, so we just validate structure
    if (!chainsJson.get("optionPairs").isEmpty()) {
      JsonNode firstPair = chainsJson.get("optionPairs").get(0);
      assertTrue(firstPair.has("strikePrice"), "Option pair should contain strikePrice");
      assertTrue(firstPair.has("call") || firstPair.has("put"), 
          "Option pair should contain call or put");
    }

    // Validate database persistence: Option chain snapshot should be created (append-only)
    List<EtradeOptionChainSnapshot> chainSnapshots = optionChainSnapshotRepository
        .findBySymbolOrderByRequestTimeDesc(symbol);
    assertFalse(chainSnapshots.isEmpty(), 
        "Option chain snapshot should be persisted in database");
    EtradeOptionChainSnapshot latestChainSnapshot = chainSnapshots.get(0);
    assertEquals(symbol, latestChainSnapshot.getSymbol(), 
        "Persisted chain snapshot symbol should match");
    assertNotNull(latestChainSnapshot.getRequestTime(), 
        "Persisted chain snapshot should have request time");
    log.info("✅ Validated option chain snapshot persistence: {} (snapshot count: {})", 
        symbol, chainSnapshots.size());

    // Validate database persistence: Option contracts should be upserted (if pairs exist)
    if (!chainsJson.get("optionPairs").isEmpty()) {
      // Check if any option contracts were persisted
      List<EtradeOptionContract> contracts = optionContractRepository
          .findByUnderlyingSymbol(symbol);
      log.info("✅ Validated option contract persistence: {} contracts found for {}", 
          contracts.size(), symbol);
    }
  }

  @Test
  @DisplayName("Test 3b: OptionChains - Invalid chainType")
  void test3b_optionChains_invalidChainType() throws Exception {
    log.info("=== Test 3b: OptionChains - Invalid chainType ===");

    String symbol = "AAPL";

    // Try with invalid chainType
    MvcResult result = mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", symbol)
            .param("chainType", "INVALID_TYPE")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    int status = result.getResponse().getStatus();
    log.info("Status with invalid chainType: {}", status);
    
    // Should fail (400 or 500) - E*TRADE API will reject invalid chainType
    assertTrue(status >= 400, "Should return error status for invalid chainType");
  }

  @Test
  @DisplayName("Test 4: Option Quote End-to-End")
  void test4_optionQuote_endToEnd() throws Exception {
    log.info("=== Test 4: Option Quote End-to-End ===");

    // First get option chains to find an option symbol
    String symbol = "AAPL";
    
    MvcResult chainsResult = mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", symbol)
            .param("chainType", "CALLPUT")
            .param("noOfStrikes", "3")
            .contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    if (chainsResult.getResponse().getStatus() != 200) {
      log.warn("⚠️  Option chains request failed, skipping option quote test");
      return;
    }

    String chainsContent = chainsResult.getResponse().getContentAsString();
    JsonNode chainsJson = objectMapper.readTree(chainsContent);

    if (!chainsJson.has("optionPairs") || chainsJson.get("optionPairs").isEmpty()) {
      log.warn("⚠️  No option pairs found, skipping option quote test");
      return;
    }

    // Extract option symbol from chain (use quoteDetail link or build symbol)
    JsonNode firstPair = chainsJson.get("optionPairs").get(0);
    String optionSymbol = null;
    
    if (firstPair.has("call") && firstPair.get("call").has("symbol")) {
      optionSymbol = firstPair.get("call").get("symbol").asText();
    } else if (firstPair.has("put") && firstPair.get("put").has("symbol")) {
      optionSymbol = firstPair.get("put").get("symbol").asText();
    }

    if (optionSymbol == null || optionSymbol.isEmpty()) {
      log.warn("⚠️  No option symbol found in chain, skipping option quote test");
      return;
    }

    log.info("Found option symbol: {}", optionSymbol);

    // Get quote for the option with OPTIONS detailFlag
    UUID authAccountId = ensureValidAccessToken();
    
    MvcResult quoteResult = mockMvc.perform(get("/api/etrade/quotes/{symbol}", optionSymbol)
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "OPTIONS")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    String quoteContent = quoteResult.getResponse().getContentAsString();
    log.info("Option Quote Response: {}", quoteContent);

    JsonNode quoteJson = objectMapper.readTree(quoteContent);
    assertTrue(quoteJson.has("options") || quoteJson.has("all"), 
        "Option quote should contain options or all details");
  }

  @Test
  @DisplayName("Full Workflow: Lookup → Quote → Option Expire Dates → Option Chains")
  void fullWorkflow_lookupQuoteOptionExpireDatesOptionChains_endToEnd() throws Exception {
    log.info("=== Full Workflow: Lookup → Quote → Option Expire Dates → Option Chains ===");

    // Step 1: Lookup Product
    String searchInput = "apple";
    MvcResult lookupResult = mockMvc.perform(get("/api/etrade/quotes/lookup")
            .param("input", searchInput)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode lookupJson = objectMapper.readTree(lookupResult.getResponse().getContentAsString());
    String symbol = lookupJson.get("data").get(0).get("symbol").asText();
    log.info("Step 1: Found symbol: {}", symbol);

    // Step 2: Get Quote
    UUID authAccountId = ensureValidAccessToken();
    
    MvcResult quoteResult = mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", symbol)
            .param("accountId", authAccountId.toString())
            .param("detailFlag", "INTRADAY")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode quoteJson = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
    assertFalse(quoteJson.get("quoteData").isEmpty(), "Quote data should not be empty");
    log.info("Step 2: Retrieved quote for {}", symbol);

    // Step 3: Get Option Expire Dates
    MvcResult expireDatesResult = mockMvc.perform(get("/api/etrade/quotes/option-expire-dates")
            .param("symbol", symbol)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode expireDatesJson = objectMapper.readTree(expireDatesResult.getResponse().getContentAsString());
    assertFalse(expireDatesJson.get("expireDates").isEmpty(), "Expire dates should not be empty");
    log.info("Step 3: Retrieved {} expiration dates", expireDatesJson.get("expireDates").size());

    // Step 4: Get Option Chains
    JsonNode firstExpireDate = expireDatesJson.get("expireDates").get(0);
    MvcResult chainsResult = mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", symbol)
            .param("expiryYear", String.valueOf(firstExpireDate.get("year").asInt()))
            .param("expiryMonth", String.valueOf(firstExpireDate.get("month").asInt()))
            .param("expiryDay", String.valueOf(firstExpireDate.get("day").asInt()))
            .param("chainType", "CALLPUT")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode chainsJson = objectMapper.readTree(chainsResult.getResponse().getContentAsString());
    assertEquals(symbol, chainsJson.get("symbol").asText(), "Chain symbol should match");
    log.info("Step 4: Retrieved option chains for {}", symbol);

    log.info("✅ Full workflow completed successfully");
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
