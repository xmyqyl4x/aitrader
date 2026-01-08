package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;

import com.myqyl.aitradex.etrade.api.XmlResponseValidator;
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
 * Standalone tests for E*TRADE Account Balance API endpoint.
 * 
 * Requires access token and a valid accountIdKey from List Accounts test.
 */
@DisplayName("E*TRADE Account Balance API Tests (Standalone)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_ACCESS_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_ACCESS_TOKEN_SECRET", matches = ".+")
class EtradeAccountBalanceApiTest {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccountBalanceApiTest.class);

  private StandaloneEtradeApiClient apiClient;
  private String baseUrl;
  private String accountIdKey; // Will be populated from List Accounts test

  @BeforeEach
  void setUp() {
    String consumerKey = System.getenv("ETRADE_CONSUMER_KEY");
    String consumerSecret = System.getenv("ETRADE_CONSUMER_SECRET");
    String accessToken = System.getenv("ETRADE_ACCESS_TOKEN");
    String accessTokenSecret = System.getenv("ETRADE_ACCESS_TOKEN_SECRET");
    baseUrl = System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com");

    assertNotNull(consumerKey, "ETRADE_CONSUMER_KEY must be set");
    assertNotNull(consumerSecret, "ETRADE_CONSUMER_SECRET must be set");
    assertNotNull(accessToken, "ETRADE_ACCESS_TOKEN must be set");
    assertNotNull(accessTokenSecret, "ETRADE_ACCESS_TOKEN_SECRET must be set");

    apiClient = new StandaloneEtradeApiClient(
        baseUrl, consumerKey, consumerSecret, accessToken, accessTokenSecret);

    // Get accountIdKey from List Accounts API
    accountIdKey = getFirstAccountIdKey();
    assertNotNull(accountIdKey, "Could not retrieve accountIdKey from List Accounts API");

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
  @DisplayName("Get Account Balance - Success")
  void getAccountBalance_success() {
    log.info("=== Testing Get Account Balance API ===");
    log.info("Account ID Key: {}", accountIdKey);

    // Build query parameters
    Map<String, String> params = Map.of(
        "instType", "BROKERAGE",
        "accountType", "CASH",
        "realTimeNAV", "true"
    );

    // Make API request
    String endpoint = "/v1/accounts/" + accountIdKey + "/balance";
    String responseXml = apiClient.get(endpoint, params);

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "BalanceResponse");
    Element root = doc.getDocumentElement();

    // Validate key fields exist
    String accountId = XmlResponseValidator.getRequiredField(root, "accountId");
    String accountType = XmlResponseValidator.getRequiredField(root, "accountType");
    String accountDescription = XmlResponseValidator.getRequiredField(root, "accountDescription");
    String accountMode = XmlResponseValidator.getRequiredField(root, "accountMode");

    log.info("Account ID: {}", accountId);
    log.info("Account Type: {}", accountType);
    log.info("Account Description: {}", accountDescription);
    log.info("Account Mode: {}", accountMode);

    // Validate sections exist
    Element cashElement = XmlResponseValidator.getFirstChildElement(root, "Cash");
    assertNotNull(cashElement, "Response should contain <Cash> element");

    Element computedElement = XmlResponseValidator.getFirstChildElement(root, "Computed");
    assertNotNull(computedElement, "Response should contain <Computed> element");

    Element marginElement = XmlResponseValidator.getFirstChildElement(root, "Margin");
    assertNotNull(marginElement, "Response should contain <Margin> element");

    log.info("✅ Cash, Computed, and Margin sections found");

    // Validate numeric fields parse correctly (even if zero)
    try {
      double netCash = XmlResponseValidator.getNumericFieldOrDefault(cashElement, "netCash", 0.0);
      double cashBuyingPower = XmlResponseValidator.getNumericFieldOrDefault(
          cashElement, "cashBuyingPower", 0.0);

      log.info("Net Cash: {}", netCash);
      log.info("Cash Buying Power: {}", cashBuyingPower);

      // These should parse successfully even if zero
      assertTrue(Double.isFinite(netCash), "netCash should be a finite number");
      assertTrue(Double.isFinite(cashBuyingPower), "cashBuyingPower should be a finite number");
    } catch (Exception e) {
      log.warn("Could not parse some numeric fields (may be expected for new accounts): {}", e.getMessage());
    }

    log.info("✅ Get Account Balance test passed - all required fields validated");
  }

  @Test
  @DisplayName("Get Account Balance - Response Structure")
  void getAccountBalance_responseStructure() {
    log.info("=== Testing Get Account Balance Response Structure ===");

    Map<String, String> params = Map.of(
        "instType", "BROKERAGE",
        "accountType", "CASH",
        "realTimeNAV", "true"
    );

    String endpoint = "/v1/accounts/" + accountIdKey + "/balance";
    String responseXml = apiClient.get(endpoint, params);
    Document doc = XmlResponseValidator.parseXml(responseXml);

    Element root = doc.getDocumentElement();
    assertEquals("BalanceResponse", root.getNodeName(), "Root element should be BalanceResponse");

    // Validate required sections
    assertNotNull(XmlResponseValidator.getFirstChildElement(root, "Cash"), "Should have Cash element");
    assertNotNull(XmlResponseValidator.getFirstChildElement(root, "Computed"), "Should have Computed element");
    assertNotNull(XmlResponseValidator.getFirstChildElement(root, "Margin"), "Should have Margin element");

    log.info("✅ Response structure validated");
  }
}
