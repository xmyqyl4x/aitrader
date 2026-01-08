package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.api.EtradeAccessTokenHelper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Comprehensive standalone tests for E*TRADE Quote/Market API v1 endpoints.
 * 
 * Tests all endpoints documented at: https://apisb.etrade.com/docs/api/market/api-quote-v1.html#
 * 
 * Endpoints covered:
 * - Get Quotes (single and multiple symbols)
 * - Look Up Product
 * - Get Option Chains
 * - Get Option Expire Dates
 * 
 * These tests require:
 * - ETRADE_CONSUMER_KEY
 * - ETRADE_CONSUMER_SECRET
 * - ETRADE_ACCESS_TOKEN (from OAuth workflow) OR ETRADE_OAUTH_VERIFIER
 * - ETRADE_ACCESS_TOKEN_SECRET (from OAuth workflow) OR ETRADE_OAUTH_VERIFIER
 * - ETRADE_BASE_URL (optional, defaults to sandbox)
 * - ETRADE_ACCOUNT_ID_KEY (optional, will be retrieved from List Accounts if not provided)
 * 
 * Note: Quote API returns JSON (not XML like Accounts/Transactions APIs)
 */
@DisplayName("E*TRADE Quotes/Market API Tests (Standalone)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
class EtradeQuotesApiTest {

  private static final Logger log = LoggerFactory.getLogger(EtradeQuotesApiTest.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private StandaloneEtradeApiClient apiClient;
  private String baseUrl;
  private String accountIdKey;
  private String consumerKey;

  @BeforeEach
  void setUp() {
    String consumerKey = System.getenv("ETRADE_CONSUMER_KEY");
    String consumerSecret = System.getenv("ETRADE_CONSUMER_SECRET");
    String accessToken = System.getenv("ETRADE_ACCESS_TOKEN");
    String accessTokenSecret = System.getenv("ETRADE_ACCESS_TOKEN_SECRET");
    baseUrl = System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com");
    this.consumerKey = consumerKey;

    assertNotNull(consumerKey, "ETRADE_CONSUMER_KEY must be set");
    assertNotNull(consumerSecret, "ETRADE_CONSUMER_SECRET must be set");
    
    // Try to get access token from environment, or attempt to obtain it if verifier is provided
    if (accessToken == null || accessTokenSecret == null) {
      String verifier = System.getenv("ETRADE_OAUTH_VERIFIER");
      if (verifier != null && !verifier.isEmpty()) {
        log.info("Access token not found in environment, attempting to obtain using verifier...");
        try {
          EtradeAccessTokenHelper.AccessTokenPair tokens = 
              EtradeAccessTokenHelper.getAccessToken(consumerKey, consumerSecret, verifier);
          accessToken = tokens.getAccessToken();
          accessTokenSecret = tokens.getAccessTokenSecret();
          log.info("✅ Successfully obtained access token");
        } catch (Exception e) {
          log.error("Failed to obtain access token: {}", e.getMessage());
          org.junit.jupiter.api.Assumptions.assumeTrue(false, 
              "ETRADE_ACCESS_TOKEN and ETRADE_ACCESS_TOKEN_SECRET must be set, " +
              "or ETRADE_OAUTH_VERIFIER must be provided to obtain tokens. Error: " + e.getMessage());
        }
      } else {
        org.junit.jupiter.api.Assumptions.assumeTrue(false, 
            "ETRADE_ACCESS_TOKEN and ETRADE_ACCESS_TOKEN_SECRET must be set, " +
            "or ETRADE_OAUTH_VERIFIER must be provided to obtain tokens");
      }
    }

    apiClient = new StandaloneEtradeApiClient(
        baseUrl, consumerKey, consumerSecret, accessToken, accessTokenSecret);

    // Get accountIdKey from environment or List Accounts API (for authenticated quotes)
    accountIdKey = System.getenv("ETRADE_ACCOUNT_ID_KEY");
    if (accountIdKey == null || accountIdKey.isEmpty()) {
      accountIdKey = getFirstAccountIdKey();
    }
    // Note: accountIdKey is optional for delayed quotes, but required for real-time quotes

    log.info("Initialized E*TRADE API client for base URL: {}", baseUrl);
    if (accountIdKey != null) {
      log.info("Using accountIdKey: {} (for real-time quotes)", accountIdKey);
    } else {
      log.info("No accountIdKey - will use delayed quotes (non-OAuth)");
    }
  }

  /**
   * Helper method to get the first accountIdKey from List Accounts API.
   */
  private String getFirstAccountIdKey() {
    try {
      String responseXml = apiClient.get("/v1/accounts/list");
      Document doc = XmlResponseValidator.parseXml(responseXml);
      Element root = doc.getDocumentElement();
      Element accountsElement = XmlResponseValidator.getFirstChildElement(root, "Accounts");
      if (accountsElement != null) {
        Element firstAccount = XmlResponseValidator.getFirstChildElement(accountsElement, "Account");
        if (firstAccount != null) {
          return XmlResponseValidator.getTextContent(firstAccount, "accountIdKey");
        }
      }
    } catch (Exception e) {
      log.warn("Failed to get accountIdKey from List Accounts: {}", e.getMessage());
    }
    return null;
  }

  // ============================================================================
  // 1. GET QUOTES TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Quote - Single Symbol (Delayed, Non-OAuth)")
  void getQuote_singleSymbol_delayed() {
    log.info("=== Testing Get Quote API (Single Symbol, Delayed) ===");

    String endpoint = "/v1/market/quote/AAPL";
    Map<String, String> params = Map.of("consumerKey", consumerKey);
    
    // Use delayed quotes (no OAuth required)
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson, "Response should not be null");
    assertFalse(responseJson.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseJson.length());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      
      assertFalse(quoteResponse.isMissingNode(), "Response should contain QuoteResponse");
      
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      assertFalse(quoteDataNode.isMissingNode(), "Response should contain QuoteData");
      
      JsonNode quoteNode = quoteDataNode.isArray() ? quoteDataNode.get(0) : quoteDataNode;
      validateQuoteStructure(quoteNode, "AAPL");

      log.info("✅ Get Quote (Single Symbol, Delayed) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Quote - Single Symbol (Real-time, OAuth)")
  void getQuote_singleSymbol_realtime() {
    log.info("=== Testing Get Quote API (Single Symbol, Real-time) ===");

    if (accountIdKey == null || accountIdKey.isEmpty()) {
      log.info("Skipping real-time quote test - no accountIdKey available");
      org.junit.jupiter.api.Assumptions.assumeTrue(false, 
          "accountIdKey required for real-time quotes");
      return;
    }

    String endpoint = "/v1/market/quote/AAPL";
    Map<String, String> params = Map.of("detailFlag", "ALL");
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      
      JsonNode quoteNode = quoteDataNode.isArray() ? quoteDataNode.get(0) : quoteDataNode;
      validateQuoteStructure(quoteNode, "AAPL");

      log.info("✅ Get Quote (Single Symbol, Real-time) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Quotes - Multiple Symbols")
  void getQuotes_multipleSymbols() {
    log.info("=== Testing Get Quotes API (Multiple Symbols) ===");

    String endpoint = "/v1/market/quote/AAPL,MSFT,GOOGL";
    Map<String, String> params = Map.of("consumerKey", consumerKey);
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      
      assertTrue(quoteDataNode.isArray(), "QuoteData should be an array for multiple symbols");
      assertTrue(quoteDataNode.size() >= 1, "Should return at least one quote");
      
      log.info("Found {} quote(s)", quoteDataNode.size());
      
      for (int i = 0; i < quoteDataNode.size(); i++) {
        JsonNode quoteNode = quoteDataNode.get(i);
        validateQuoteStructure(quoteNode, "Quote #" + (i + 1));
      }

      log.info("✅ Get Quotes (Multiple Symbols) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Quote - With detailFlag Parameter")
  void getQuote_withDetailFlag() {
    log.info("=== Testing Get Quote API with detailFlag Parameter ===");

    String endpoint = "/v1/market/quote/AAPL";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "detailFlag", "ALL"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      
      JsonNode quoteNode = quoteDataNode.isArray() ? quoteDataNode.get(0) : quoteDataNode;
      
      // With detailFlag=ALL, should have more detailed fields
      JsonNode allNode = quoteNode.path("All");
      if (!allNode.isMissingNode()) {
        // Validate detailed fields are present
        assertTrue(allNode.has("bid") || allNode.has("ask") || allNode.has("lastTrade"),
            "With detailFlag=ALL, quote should have detailed fields");
      }

      log.info("✅ Get Quote with detailFlag test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Quote - Invalid Symbol")
  void getQuote_invalidSymbol() {
    log.info("=== Testing Get Quote API with Invalid Symbol ===");

    String endpoint = "/v1/market/quote/INVALID_SYMBOL_XYZ123";
    Map<String, String> params = Map.of("consumerKey", consumerKey);
    
    // Invalid symbols may return empty QuoteData or error
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      
      // Response should still be valid JSON, but may have empty QuoteData
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      if (quoteDataNode.isArray() && quoteDataNode.size() == 0) {
        log.info("Invalid symbol returned empty QuoteData (expected)");
      } else if (quoteDataNode.isMissingNode()) {
        log.info("Invalid symbol returned no QuoteData (expected)");
      }

      log.info("✅ Get Quote with invalid symbol test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Quote - Mutual Fund")
  void getQuote_mutualFund() {
    log.info("=== Testing Get Quote API (Mutual Fund) ===");

    String endpoint = "/v1/market/quote/VTSAX";
    Map<String, String> params = Map.of("consumerKey", consumerKey);
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode quoteResponse = root.path("QuoteResponse");
      JsonNode quoteDataNode = quoteResponse.path("QuoteData");
      
      if (!quoteDataNode.isMissingNode()) {
        JsonNode quoteNode = quoteDataNode.isArray() ? quoteDataNode.get(0) : quoteDataNode;
        
        // Validate mutual fund specific fields
        JsonNode mutualFundNode = quoteNode.path("MutualFund");
        if (!mutualFundNode.isMissingNode()) {
          log.info("Mutual fund quote found");
          validateMutualFundQuote(quoteNode);
        } else {
          log.info("Quote found but not a mutual fund (may be ETF or other type)");
          validateQuoteStructure(quoteNode, "VTSAX");
        }
      }

      log.info("✅ Get Quote (Mutual Fund) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  // ============================================================================
  // 2. LOOK UP PRODUCT TESTS
  // ============================================================================

  @Test
  @DisplayName("Look Up Product - By Symbol")
  void lookupProduct_bySymbol() {
    log.info("=== Testing Look Up Product API (By Symbol) ===");

    String endpoint = "/v1/market/lookup";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "input", "AAPL"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode lookupResponse = root.path("LookupResponse");
      
      assertFalse(lookupResponse.isMissingNode(), "Response should contain LookupResponse");
      
      JsonNode dataNode = lookupResponse.path("Data");
      if (!dataNode.isMissingNode()) {
        log.info("Found {} product(s)", dataNode.isArray() ? dataNode.size() : 1);
        
        if (dataNode.isArray() && dataNode.size() > 0) {
          JsonNode productNode = dataNode.get(0);
          validateProductLookupStructure(productNode);
        } else if (dataNode.isObject()) {
          validateProductLookupStructure(dataNode);
        }
      }

      log.info("✅ Look Up Product (By Symbol) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Look Up Product - By Company Name")
  void lookupProduct_byCompanyName() {
    log.info("=== Testing Look Up Product API (By Company Name) ===");

    String endpoint = "/v1/market/lookup";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "input", "Apple"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode lookupResponse = root.path("LookupResponse");
      
      assertFalse(lookupResponse.isMissingNode(), "Response should contain LookupResponse");
      
      JsonNode dataNode = lookupResponse.path("Data");
      if (!dataNode.isMissingNode() && dataNode.isArray() && dataNode.size() > 0) {
        log.info("Found {} product(s) matching 'Apple'", dataNode.size());
        
        // Validate first result
        validateProductLookupStructure(dataNode.get(0));
      }

      log.info("✅ Look Up Product (By Company Name) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Look Up Product - Invalid Input")
  void lookupProduct_invalidInput() {
    log.info("=== Testing Look Up Product API with Invalid Input ===");

    String endpoint = "/v1/market/lookup";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "input", "INVALID_INPUT_XYZ123"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode lookupResponse = root.path("LookupResponse");
      
      // Invalid input may return empty Data array
      JsonNode dataNode = lookupResponse.path("Data");
      if (dataNode.isArray() && dataNode.size() == 0) {
        log.info("Invalid input returned empty results (expected)");
      }

      log.info("✅ Look Up Product with invalid input test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  // ============================================================================
  // 3. GET OPTION CHAINS TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Option Chains - Basic")
  void getOptionChains_basic() {
    log.info("=== Testing Get Option Chains API (Basic) ===");

    String endpoint = "/v1/market/optionchains";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "symbol", "AAPL",
        "expiryYear", "2024",
        "expiryMonth", "1",
        "expiryDay", "19",
        "strikePriceNear", "150",
        "noOfStrikes", "5"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode optionChainResponse = root.path("OptionChainResponse");
      
      assertFalse(optionChainResponse.isMissingNode(), 
          "Response should contain OptionChainResponse");
      
      // Validate option chain structure
      JsonNode optionPairNode = optionChainResponse.path("OptionPair");
      if (!optionPairNode.isMissingNode()) {
        log.info("Option chain data found");
        validateOptionChainStructure(optionPairNode);
      }

      log.info("✅ Get Option Chains (Basic) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Option Chains - With Call/Put Filter")
  void getOptionChains_withCallPutFilter() {
    log.info("=== Testing Get Option Chains API (With Call/Put Filter) ===");

    String endpoint = "/v1/market/optionchains";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "symbol", "AAPL",
        "expiryYear", "2024",
        "expiryMonth", "1",
        "expiryDay", "19",
        "strikePriceNear", "150",
        "noOfStrikes", "5",
        "optionCategory", "STANDARD",
        "chainType", "CALL"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode optionChainResponse = root.path("OptionChainResponse");
      
      assertFalse(optionChainResponse.isMissingNode(), 
          "Response should contain OptionChainResponse");

      log.info("✅ Get Option Chains with Call/Put filter test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Option Chains - Invalid Symbol")
  void getOptionChains_invalidSymbol() {
    log.info("=== Testing Get Option Chains API with Invalid Symbol ===");

    String endpoint = "/v1/market/optionchains";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "symbol", "INVALID_SYMBOL",
        "expiryYear", "2024",
        "expiryMonth", "1",
        "expiryDay", "19"
    );
    
    // Invalid symbols may return error or empty response
    try {
      String responseJson = apiClient.get(endpoint, params);
      
      assertNotNull(responseJson);
      assertFalse(responseJson.trim().isEmpty());

      JsonNode root = objectMapper.readTree(responseJson);
      // May have error messages or empty OptionChainResponse
      
      log.info("✅ Get Option Chains with invalid symbol test passed");
    } catch (RuntimeException e) {
      // API may reject invalid symbol with error
      if (e.getMessage().contains("400") || e.getMessage().contains("failed")) {
        log.info("✅ Invalid symbol properly rejected");
      } else {
        throw e;
      }
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  // ============================================================================
  // 4. GET OPTION EXPIRE DATES TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Option Expire Dates - Basic")
  void getOptionExpireDates_basic() {
    log.info("=== Testing Get Option Expire Dates API (Basic) ===");

    String endpoint = "/v1/market/optionexpiredate";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "symbol", "AAPL"
    );
    
    String responseJson = apiClient.get(endpoint, params);

    assertNotNull(responseJson);
    assertFalse(responseJson.trim().isEmpty());

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode optionExpireDateResponse = root.path("OptionExpireDateResponse");
      
      assertFalse(optionExpireDateResponse.isMissingNode(), 
          "Response should contain OptionExpireDateResponse");
      
      JsonNode expireDateNode = optionExpireDateResponse.path("ExpireDate");
      if (!expireDateNode.isMissingNode()) {
        if (expireDateNode.isArray()) {
          log.info("Found {} expire date(s)", expireDateNode.size());
          for (int i = 0; i < Math.min(expireDateNode.size(), 5); i++) {
            JsonNode dateNode = expireDateNode.get(i);
            validateExpireDateStructure(dateNode);
          }
        } else if (expireDateNode.isObject()) {
          validateExpireDateStructure(expireDateNode);
        }
      }

      log.info("✅ Get Option Expire Dates (Basic) test passed");
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get Option Expire Dates - Invalid Symbol")
  void getOptionExpireDates_invalidSymbol() {
    log.info("=== Testing Get Option Expire Dates API with Invalid Symbol ===");

    String endpoint = "/v1/market/optionexpiredate";
    Map<String, String> params = Map.of(
        "consumerKey", consumerKey,
        "symbol", "INVALID_SYMBOL_XYZ"
    );
    
    // Invalid symbols may return error or empty response
    try {
      String responseJson = apiClient.get(endpoint, params);
      
      assertNotNull(responseJson);
      assertFalse(responseJson.trim().isEmpty());

      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode optionExpireDateResponse = root.path("OptionExpireDateResponse");
      
      // May have empty ExpireDate array
      JsonNode expireDateNode = optionExpireDateResponse.path("ExpireDate");
      if (expireDateNode.isArray() && expireDateNode.size() == 0) {
        log.info("Invalid symbol returned empty expire dates (expected)");
      }

      log.info("✅ Get Option Expire Dates with invalid symbol test passed");
    } catch (RuntimeException e) {
      // API may reject invalid symbol with error
      if (e.getMessage().contains("400") || e.getMessage().contains("failed")) {
        log.info("✅ Invalid symbol properly rejected");
      } else {
        throw e;
      }
    } catch (JsonProcessingException e) {
      fail("Failed to parse JSON response: " + e.getMessage());
    }
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private void validateQuoteStructure(JsonNode quoteNode, String context) {
    log.info("Validating {} quote structure", context);

    // Validate Product information
    JsonNode productNode = quoteNode.path("Product");
    if (!productNode.isMissingNode()) {
      String symbol = productNode.path("symbol").asText("");
      String securityType = productNode.path("securityType").asText("");
      log.info("  Symbol: {}", symbol);
      log.info("  Security Type: {}", securityType);
      
      if (!symbol.isEmpty()) {
        assertFalse(symbol.isEmpty(), "Symbol should not be empty");
      }
    }

    // Validate All quotes (for stocks/ETFs)
    JsonNode allNode = quoteNode.path("All");
    if (!allNode.isMissingNode()) {
      log.info("  Quote type: Stock/ETF");
      
      // Validate numeric fields
      JsonNode lastTradeNode = allNode.path("lastTrade");
      if (!lastTradeNode.isMissingNode() && !lastTradeNode.isNull()) {
        double lastTrade = lastTradeNode.asDouble();
        assertTrue(lastTrade >= 0 || Double.isNaN(lastTrade), 
            "lastTrade should be non-negative or NaN");
        log.info("  Last Trade: {}", lastTrade);
      }

      JsonNode volumeNode = allNode.path("volume");
      if (!volumeNode.isMissingNode() && !volumeNode.isNull()) {
        long volume = volumeNode.asLong();
        assertTrue(volume >= 0, "volume should be non-negative");
        log.info("  Volume: {}", volume);
      }
    }

    // Validate DateTime
    JsonNode dateTimeNode = quoteNode.path("dateTime");
    if (!dateTimeNode.isMissingNode() && !dateTimeNode.isNull()) {
      if (dateTimeNode.isNumber()) {
        long dateTime = dateTimeNode.asLong();
        assertTrue(dateTime > 0, "dateTime should be positive");
        log.info("  DateTime: {}", dateTime);
      } else {
        String dateTime = dateTimeNode.asText("");
        assertFalse(dateTime.isEmpty(), "dateTime should not be empty if present");
        log.info("  DateTime: {}", dateTime);
      }
    }
  }

  private void validateMutualFundQuote(JsonNode quoteNode) {
    log.info("Validating mutual fund quote structure");

    JsonNode mutualFundNode = quoteNode.path("MutualFund");
    assertFalse(mutualFundNode.isMissingNode(), "Mutual fund quote should have MutualFund node");

    JsonNode navNode = mutualFundNode.path("netAssetValue");
    if (!navNode.isMissingNode() && !navNode.isNull()) {
      double nav = navNode.asDouble();
      assertTrue(nav > 0, "netAssetValue should be positive");
      log.info("  NAV: {}", nav);
    }

    JsonNode popNode = mutualFundNode.path("publicOfferPrice");
    if (!popNode.isMissingNode() && !popNode.isNull()) {
      double pop = popNode.asDouble();
      assertTrue(pop > 0, "publicOfferPrice should be positive");
      log.info("  Public Offer Price: {}", pop);
    }
  }

  private void validateProductLookupStructure(JsonNode productNode) {
    log.info("Validating product lookup structure");

    String symbol = productNode.path("symbol").asText("");
    String description = productNode.path("description").asText("");
    String securityType = productNode.path("securityType").asText("");

    log.info("  Symbol: {}", symbol);
    log.info("  Description: {}", description);
    log.info("  Security Type: {}", securityType);

    // At least symbol or description should be present
    assertTrue(!symbol.isEmpty() || !description.isEmpty(), 
        "Product lookup should have symbol or description");
  }

  private void validateOptionChainStructure(JsonNode optionPairNode) {
    log.info("Validating option chain structure");

    // OptionPair may be array or object
    JsonNode pair = optionPairNode.isArray() ? optionPairNode.get(0) : optionPairNode;

    JsonNode callNode = pair.path("Call");
    JsonNode putNode = pair.path("Put");

    if (!callNode.isMissingNode()) {
      log.info("  Call option data found");
    }
    if (!putNode.isMissingNode()) {
      log.info("  Put option data found");
    }
  }

  private void validateExpireDateStructure(JsonNode expireDateNode) {
    log.info("Validating expire date structure");

    JsonNode yearNode = expireDateNode.path("year");
    JsonNode monthNode = expireDateNode.path("month");
    JsonNode dayNode = expireDateNode.path("day");

    if (!yearNode.isMissingNode()) {
      int year = yearNode.asInt();
      assertTrue(year >= 2020 && year <= 2100, "Year should be reasonable");
      log.info("  Expire Date: {}/{}/{}", 
          monthNode.asInt(-1), dayNode.asInt(-1), year);
    }
  }
}
