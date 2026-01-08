package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;

import com.myqyl.aitradex.etrade.api.EtradeAccessTokenHelper;
import com.myqyl.aitradex.etrade.api.XmlResponseValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Standalone tests for E*TRADE Accounts API endpoints.
 * 
 * These tests require:
 * - ETRADE_CONSUMER_KEY
 * - ETRADE_CONSUMER_SECRET
 * - ETRADE_ACCESS_TOKEN (from OAuth workflow)
 * - ETRADE_ACCESS_TOKEN_SECRET (from OAuth workflow)
 * - ETRADE_BASE_URL (optional, defaults to sandbox)
 * 
 * To obtain access token:
 * 1. Run EtradeOAuthWorkflowStandaloneTest to get authorization URL
 * 2. Authorize in browser and get oauth_verifier
 * 3. Run EtradeOAuthWorkflowIntegrationTest#step3_accessTokenExchange_manualVerifier with verifier
 * 4. Extract access token and secret from test output or database
 */
@DisplayName("E*TRADE Accounts API Tests (Standalone)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
// Note: Tests require either:
// - ETRADE_ACCESS_TOKEN and ETRADE_ACCESS_TOKEN_SECRET (direct tokens)
// - OR ETRADE_OAUTH_VERIFIER (to obtain tokens automatically)
// Tests will be skipped if neither is provided
class EtradeAccountsApiTest {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountsApiTest.class);

  private StandaloneEtradeApiClient apiClient;
  private String baseUrl;

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

    log.info("Initialized E*TRADE API client for base URL: {}", baseUrl);
  }

  @Test
  @DisplayName("List Accounts - Success")
  void listAccounts_success() {
    log.info("=== Testing List Accounts API ===");

    // Make API request
    String responseXml = apiClient.get("/v1/accounts/list");

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "AccountListResponse");
    Element root = doc.getDocumentElement();

    // Validate Accounts element exists
    Element accountsElement = XmlResponseValidator.getFirstChildElement(root, "Accounts");
    assertNotNull(accountsElement, "Response should contain <Accounts> element");

    // Get all Account elements
    List<Element> accountElements = XmlResponseValidator.getChildElements(accountsElement, "Account");
    assertFalse(accountElements.isEmpty(), "Response should contain at least one <Account>");

    log.info("Found {} account(s)", accountElements.size());

    // Validate required fields for each account
    for (int i = 0; i < accountElements.size(); i++) {
      Element account = accountElements.get(i);
      log.info("Validating account #{}", i + 1);

      // Validate required fields
      String accountId = XmlResponseValidator.getRequiredField(account, "accountId");
      String accountIdKey = XmlResponseValidator.getRequiredField(account, "accountIdKey");
      String accountMode = XmlResponseValidator.getRequiredField(account, "accountMode");
      String accountDesc = XmlResponseValidator.getRequiredField(account, "accountDesc");
      String accountName = XmlResponseValidator.getRequiredField(account, "accountName");
      String accountType = XmlResponseValidator.getRequiredField(account, "accountType");
      String institutionType = XmlResponseValidator.getRequiredField(account, "institutionType");
      String accountStatus = XmlResponseValidator.getRequiredField(account, "accountStatus");

      log.info("  Account ID: {}", accountId);
      log.info("  Account ID Key: {}", accountIdKey);
      log.info("  Account Mode: {}", accountMode);
      log.info("  Account Description: {}", accountDesc);
      log.info("  Account Name: {}", accountName);
      log.info("  Account Type: {}", accountType);
      log.info("  Institution Type: {}", institutionType);
      log.info("  Account Status: {}", accountStatus);

      // Additional validations
      assertFalse(accountId.trim().isEmpty(), "accountId should not be empty");
      assertFalse(accountIdKey.trim().isEmpty(), "accountIdKey should not be empty");
    }

    log.info("✅ List Accounts test passed - all required fields validated");
  }

  @Test
  @DisplayName("List Accounts - Response Structure")
  void listAccounts_responseStructure() {
    log.info("=== Testing List Accounts Response Structure ===");

    String responseXml = apiClient.get("/v1/accounts/list");
    Document doc = XmlResponseValidator.parseXml(responseXml);

    Element root = doc.getDocumentElement();
    assertEquals("AccountListResponse", root.getNodeName(), "Root element should be AccountListResponse");

    Element accountsElement = XmlResponseValidator.getFirstChildElement(root, "Accounts");
    assertNotNull(accountsElement, "Should have Accounts element");

    List<Element> accountElements = XmlResponseValidator.getChildElements(accountsElement, "Account");
    assertTrue(accountElements.size() > 0, "Should have at least one Account");

    log.info("✅ Response structure validated");
  }
}
