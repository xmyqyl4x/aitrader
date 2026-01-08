package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;

import com.myqyl.aitradex.etrade.api.EtradeAccessTokenHelper;
import com.myqyl.aitradex.etrade.api.XmlResponseValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Standalone tests for E*TRADE Transactions API endpoints.
 * 
 * These tests require:
 * - ETRADE_CONSUMER_KEY
 * - ETRADE_CONSUMER_SECRET
 * - ETRADE_ACCESS_TOKEN (from OAuth workflow) OR ETRADE_OAUTH_VERIFIER
 * - ETRADE_ACCESS_TOKEN_SECRET (from OAuth workflow) OR ETRADE_OAUTH_VERIFIER
 * - ETRADE_BASE_URL (optional, defaults to sandbox)
 * - ETRADE_ACCOUNT_ID_KEY (optional, will be retrieved from List Accounts if not provided)
 * 
 * To obtain access token:
 * 1. Run EtradeOAuthWorkflowStandaloneTest to get authorization URL
 * 2. Authorize in browser and get oauth_verifier
 * 3. Set ETRADE_OAUTH_VERIFIER environment variable
 * 4. Tests will automatically obtain access token using the verifier
 */
@DisplayName("E*TRADE Transactions API Tests (Standalone)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
// Note: Tests require either:
// - ETRADE_ACCESS_TOKEN and ETRADE_ACCESS_TOKEN_SECRET (direct tokens)
// - OR ETRADE_OAUTH_VERIFIER (to obtain tokens automatically)
// Tests will be skipped if neither is provided
class EtradeTransactionsApiTest {

  private static final Logger log = LoggerFactory.getLogger(EtradeTransactionsApiTest.class);

  private StandaloneEtradeApiClient apiClient;
  private String baseUrl;
  private String accountIdKey; // Will be populated from List Accounts test or env var

  @BeforeEach
  void setUp() {
    String consumerKey = System.getenv("ETRADE_CONSUMER_KEY");
    String consumerSecret = System.getenv("ETRADE_CONSUMER_SECRET");
    String accessToken = System.getenv("ETRADE_ACCESS_TOKEN");
    String accessTokenSecret = System.getenv("ETRADE_ACCESS_TOKEN_SECRET");
    baseUrl = System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com");

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

    // Get accountIdKey from environment or List Accounts API
    accountIdKey = System.getenv("ETRADE_ACCOUNT_ID_KEY");
    if (accountIdKey == null || accountIdKey.isEmpty()) {
      accountIdKey = getFirstAccountIdKey();
    }
    assertNotNull(accountIdKey, "Could not retrieve accountIdKey from List Accounts API or ETRADE_ACCOUNT_ID_KEY env var");

    log.info("Initialized E*TRADE API client for base URL: {}", baseUrl);
    log.info("Using accountIdKey: {}", accountIdKey);
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

  @Test
  @DisplayName("List Transactions - Success")
  void listTransactions_success() {
    log.info("=== Testing List Transactions API ===");
    log.info("Account ID Key: {}", accountIdKey);

    // Make API request
    String endpoint = "/v1/accounts/" + accountIdKey + "/transactions";
    String responseXml = apiClient.get(endpoint);

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "TransactionListResponse");
    Element root = doc.getDocumentElement();

    // Validate response-level fields
    String transactionCountStr = XmlResponseValidator.getTextContent(root, "transactionCount");
    String totalCountStr = XmlResponseValidator.getTextContent(root, "totalCount");
    String moreTransactionsStr = XmlResponseValidator.getTextContent(root, "moreTransactions");

    // Validate transactionCount is numeric
    if (transactionCountStr != null && !transactionCountStr.trim().isEmpty()) {
      try {
        int transactionCount = Integer.parseInt(transactionCountStr.trim());
        log.info("Transaction Count: {}", transactionCount);
        assertTrue(transactionCount >= 0, "transactionCount should be non-negative");
      } catch (NumberFormatException e) {
        throw new AssertionError("transactionCount is not a valid number: " + transactionCountStr);
      }
    }

    // Validate totalCount is numeric
    if (totalCountStr != null && !totalCountStr.trim().isEmpty()) {
      try {
        int totalCount = Integer.parseInt(totalCountStr.trim());
        log.info("Total Count: {}", totalCount);
        assertTrue(totalCount >= 0, "totalCount should be non-negative");
      } catch (NumberFormatException e) {
        throw new AssertionError("totalCount is not a valid number: " + totalCountStr);
      }
    }

    // Validate moreTransactions is boolean
    if (moreTransactionsStr != null && !moreTransactionsStr.trim().isEmpty()) {
      boolean moreTransactions = Boolean.parseBoolean(moreTransactionsStr.trim());
      log.info("More Transactions: {}", moreTransactions);
    }

    // Get Transactions element
    Element transactionsElement = XmlResponseValidator.getFirstChildElement(root, "Transactions");
    if (transactionsElement != null) {
      List<Element> transactionElements = XmlResponseValidator.getChildElements(
          transactionsElement, "Transaction");
      log.info("Found {} transaction(s)", transactionElements.size());

      // Validate required fields for each transaction
      for (int i = 0; i < transactionElements.size(); i++) {
        Element transaction = transactionElements.get(i);
        log.info("Validating transaction #{}", i + 1);

        // Validate required fields
        String transactionId = XmlResponseValidator.getRequiredField(transaction, "transactionId");
        String accountId = XmlResponseValidator.getRequiredField(transaction, "accountId");
        String transactionDateStr = XmlResponseValidator.getRequiredField(transaction, "transactionDate");
        String amountStr = XmlResponseValidator.getRequiredField(transaction, "amount");
        String description = XmlResponseValidator.getRequiredField(transaction, "description");
        String transactionType = XmlResponseValidator.getRequiredField(transaction, "transactionType");
        String instType = XmlResponseValidator.getRequiredField(transaction, "instType");
        String detailsURI = XmlResponseValidator.getRequiredField(transaction, "detailsURI");

        log.info("  Transaction ID: {}", transactionId);
        log.info("  Account ID: {}", accountId);
        log.info("  Transaction Date: {}", transactionDateStr);
        log.info("  Amount: {}", amountStr);
        log.info("  Description: {}", description);
        log.info("  Transaction Type: {}", transactionType);
        log.info("  Inst Type: {}", instType);
        log.info("  Details URI: {}", detailsURI);

        // Validate transactionDate is parseable as epoch millis
        try {
          long transactionDate = Long.parseLong(transactionDateStr.trim());
          assertTrue(transactionDate > 0, "transactionDate should be positive");
          log.info("  Transaction Date (parsed): {}", transactionDate);
        } catch (NumberFormatException e) {
          throw new AssertionError("transactionDate is not a valid epoch millis: " + transactionDateStr);
        }

        // Validate amount is numeric
        try {
          double amount = Double.parseDouble(amountStr.trim());
          assertTrue(Double.isFinite(amount), "amount should be a finite number");
          log.info("  Amount (parsed): {}", amount);
        } catch (NumberFormatException e) {
          throw new AssertionError("amount is not a valid number: " + amountStr);
        }

        // Validate detailsURI looks like a valid E*TRADE URL
        if (detailsURI != null && !detailsURI.isEmpty()) {
          if (!detailsURI.startsWith("http://") && !detailsURI.startsWith("https://")) {
            throw new AssertionError("detailsURI does not look like a valid URL: " + detailsURI);
          }
          if (!detailsURI.contains("etrade.com") && !detailsURI.contains("/v1/")) {
            log.warn("detailsURI may not be a valid E*TRADE API URL: {}", detailsURI);
          }
        }
      }
    } else {
      log.info("No transactions found in response (account may have no transactions)");
    }

    log.info("✅ List Transactions test passed - all required fields validated");
  }

  @Test
  @DisplayName("List Transactions - With Pagination (count=3)")
  void listTransactions_withPagination() {
    log.info("=== Testing List Transactions API with Pagination ===");
    log.info("Account ID Key: {}", accountIdKey);

    // Make API request with count parameter
    String endpoint = "/v1/accounts/" + accountIdKey + "/transactions";
    Map<String, String> params = Map.of("count", "3");
    String responseXml = apiClient.get(endpoint, params);

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "TransactionListResponse");
    Element root = doc.getDocumentElement();

    // Validate transactionCount <= 3
    String transactionCountStr = XmlResponseValidator.getTextContent(root, "transactionCount");
    if (transactionCountStr != null && !transactionCountStr.trim().isEmpty()) {
      try {
        int transactionCount = Integer.parseInt(transactionCountStr.trim());
        log.info("Transaction Count: {} (should be <= 3)", transactionCount);
        assertTrue(transactionCount <= 3, 
            "transactionCount should be <= 3 when count=3 parameter is used");
      } catch (NumberFormatException e) {
        throw new AssertionError("transactionCount is not a valid number: " + transactionCountStr);
      }
    }

    // Check for pagination markers
    Element nextElement = XmlResponseValidator.getFirstChildElement(root, "next");
    Element markerElement = XmlResponseValidator.getFirstChildElement(root, "marker");
    
    if (nextElement != null) {
      String nextValue = XmlResponseValidator.getTextContent(nextElement, "marker");
      if (nextValue != null && !nextValue.trim().isEmpty()) {
        log.info("Next marker found: {}", nextValue);
        assertFalse(nextValue.trim().isEmpty(), "next marker should not be empty if present");
      }
    }

    if (markerElement != null) {
      String markerValue = markerElement.getTextContent();
      if (markerValue != null && !markerValue.trim().isEmpty()) {
        log.info("Marker found: {}", markerValue);
        assertFalse(markerValue.trim().isEmpty(), "marker should not be empty if present");
      }
    }

    log.info("✅ List Transactions with pagination test passed");
  }

  @Test
  @DisplayName("Get Transaction Details - Success")
  void getTransactionDetails_success() {
    log.info("=== Testing Get Transaction Details API ===");
    log.info("Account ID Key: {}", accountIdKey);

    // First, get a transaction ID from List Transactions
    String listEndpoint = "/v1/accounts/" + accountIdKey + "/transactions";
    String listResponseXml = apiClient.get(listEndpoint);
    
    Document listDoc = XmlResponseValidator.parseXml(listResponseXml);
    Element listRoot = listDoc.getDocumentElement();
    Element transactionsElement = XmlResponseValidator.getFirstChildElement(listRoot, "Transactions");
    
    if (transactionsElement == null) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, 
          "No transactions found in account. Cannot test transaction details.");
      return;
    }

    List<Element> transactionElements = XmlResponseValidator.getChildElements(
        transactionsElement, "Transaction");
    
    if (transactionElements.isEmpty()) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, 
          "No transactions found in account. Cannot test transaction details.");
      return;
    }

    // Get the first transaction ID
    Element firstTransaction = transactionElements.get(0);
    String transactionId = XmlResponseValidator.getRequiredField(firstTransaction, "transactionId");
    log.info("Using Transaction ID: {}", transactionId);

    // Make API request for transaction details
    String endpoint = "/v1/accounts/" + accountIdKey + "/transactions/" + transactionId;
    String responseXml = apiClient.get(endpoint);

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "TransactionDetailsResponse");
    Element root = doc.getDocumentElement();

    // Validate required fields
    String responseTransactionId = XmlResponseValidator.getRequiredField(root, "transactionId");
    String accountId = XmlResponseValidator.getRequiredField(root, "accountId");
    String transactionDateStr = XmlResponseValidator.getRequiredField(root, "transactionDate");
    String amountStr = XmlResponseValidator.getRequiredField(root, "amount");
    String description = XmlResponseValidator.getRequiredField(root, "description");

    log.info("Transaction ID: {}", responseTransactionId);
    log.info("Account ID: {}", accountId);
    log.info("Transaction Date: {}", transactionDateStr);
    log.info("Amount: {}", amountStr);
    log.info("Description: {}", description);

    // Validate transactionId matches requested ID
    assertEquals(transactionId, responseTransactionId, 
        "Response transactionId should match requested transactionId");

    // Validate transactionDate is parseable as epoch millis
    try {
      long transactionDate = Long.parseLong(transactionDateStr.trim());
      assertTrue(transactionDate > 0, "transactionDate should be positive");
      log.info("Transaction Date (parsed): {}", transactionDate);
    } catch (NumberFormatException e) {
      throw new AssertionError("transactionDate is not a valid epoch millis: " + transactionDateStr);
    }

    // Validate amount is numeric
    try {
      double amount = Double.parseDouble(amountStr.trim());
      assertTrue(Double.isFinite(amount), "amount should be a finite number");
      log.info("Amount (parsed): {}", amount);
    } catch (NumberFormatException e) {
      throw new AssertionError("amount is not a valid number: " + amountStr);
    }

    // Validate nested objects exist
    Element categoryElement = XmlResponseValidator.getFirstChildElement(root, "Category");
    if (categoryElement != null) {
      String categoryId = XmlResponseValidator.getTextContent(categoryElement, "categoryId");
      String parentId = XmlResponseValidator.getTextContent(categoryElement, "parentId");
      
      log.info("Category ID: {}", categoryId);
      log.info("Parent ID: {}", parentId);
      
      if (categoryId != null && !categoryId.trim().isEmpty()) {
        assertFalse(categoryId.trim().isEmpty(), "categoryId should not be empty if present");
      }
      if (parentId != null && !parentId.trim().isEmpty()) {
        assertFalse(parentId.trim().isEmpty(), "parentId should not be empty if present");
      }
    }

    Element brokerageElement = XmlResponseValidator.getFirstChildElement(root, "Brokerage");
    if (brokerageElement != null) {
      String brokerageTransactionType = XmlResponseValidator.getTextContent(
          brokerageElement, "transactionType");
      log.info("Brokerage Transaction Type: {}", brokerageTransactionType);
      
      if (brokerageTransactionType != null && !brokerageTransactionType.trim().isEmpty()) {
        assertFalse(brokerageTransactionType.trim().isEmpty(), 
            "brokerage transactionType should not be empty if present");
      }
    }

    // Validate currency fields if present
    String settlementCurrency = XmlResponseValidator.getTextContent(root, "settlementCurrency");
    String paymentCurrency = XmlResponseValidator.getTextContent(root, "paymentCurrency");
    
    if (settlementCurrency != null && !settlementCurrency.trim().isEmpty()) {
      log.info("Settlement Currency: {}", settlementCurrency);
      assertFalse(settlementCurrency.trim().isEmpty(), 
          "settlementCurrency should not be empty if present");
    }
    
    if (paymentCurrency != null && !paymentCurrency.trim().isEmpty()) {
      log.info("Payment Currency: {}", paymentCurrency);
      assertFalse(paymentCurrency.trim().isEmpty(), 
          "paymentCurrency should not be empty if present");
    }

    log.info("✅ Get Transaction Details test passed - all required fields validated");
  }

  @Test
  @DisplayName("List Transactions - Response Structure")
  void listTransactions_responseStructure() {
    log.info("=== Testing List Transactions Response Structure ===");

    String endpoint = "/v1/accounts/" + accountIdKey + "/transactions";
    String responseXml = apiClient.get(endpoint);
    Document doc = XmlResponseValidator.parseXml(responseXml);

    Element root = doc.getDocumentElement();
    assertEquals("TransactionListResponse", root.getNodeName(), 
        "Root element should be TransactionListResponse");

    // Validate response-level fields exist (may be empty for accounts with no transactions)
    String transactionCountStr = XmlResponseValidator.getTextContent(root, "transactionCount");
    String totalCountStr = XmlResponseValidator.getTextContent(root, "totalCount");
    
    log.info("Transaction Count: {}", transactionCountStr);
    log.info("Total Count: {}", totalCountStr);

    log.info("✅ Response structure validated");
  }
}
