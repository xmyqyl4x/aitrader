package com.myqyl.aitradex.etrade.api;

import static org.junit.jupiter.api.Assertions.*;

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
 * Standalone tests for E*TRADE Portfolio API endpoint.
 * 
 * Requires access token and a valid accountIdKey from List Accounts test.
 */
@DisplayName("E*TRADE Portfolio API Tests (Standalone)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_ACCESS_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_ACCESS_TOKEN_SECRET", matches = ".+")
class EtradePortfolioApiTest {

  private static final Logger log = LoggerFactory.getLogger(EtradePortfolioApiTest.class);

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
  @DisplayName("View Portfolio - Success")
  void viewPortfolio_success() {
    log.info("=== Testing View Portfolio API ===");
    log.info("Account ID Key: {}", accountIdKey);

    // Make API request
    String endpoint = "/v1/accounts/" + accountIdKey + "/portfolio";
    String responseXml = apiClient.get(endpoint);

    // Validate HTTP response
    assertNotNull(responseXml, "Response should not be null");
    assertFalse(responseXml.trim().isEmpty(), "Response should not be empty");

    log.info("Response received (length: {} chars)", responseXml.length());

    // Parse XML
    Document doc = XmlResponseValidator.parseXml(responseXml);

    // Validate root element
    XmlResponseValidator.validateRootElement(doc, "PortfolioResponse");
    Element root = doc.getDocumentElement();

    // Validate AccountPortfolio element exists
    Element accountPortfolioElement = XmlResponseValidator.getFirstChildElement(root, "AccountPortfolio");
    assertNotNull(accountPortfolioElement, "Response should contain <AccountPortfolio> element");

    // Validate accountId exists
    String accountId = XmlResponseValidator.getRequiredField(accountPortfolioElement, "accountId");
    log.info("Account ID: {}", accountId);

    // Validate totalPages exists and is numeric
    String totalPagesStr = XmlResponseValidator.getTextContent(accountPortfolioElement, "totalPages");
    if (totalPagesStr != null && !totalPagesStr.trim().isEmpty()) {
      try {
        int totalPages = Integer.parseInt(totalPagesStr.trim());
        log.info("Total Pages: {}", totalPages);
        assertTrue(totalPages >= 0, "totalPages should be non-negative");
      } catch (NumberFormatException e) {
        throw new AssertionError("totalPages is not a valid number: " + totalPagesStr);
      }
    } else {
      log.warn("totalPages field is missing or empty");
    }

    // Check for Position elements
    Element positionsElement = XmlResponseValidator.getFirstChildElement(accountPortfolioElement, "Positions");
    if (positionsElement != null) {
      List<Element> positionElements = XmlResponseValidator.getChildElements(positionsElement, "Position");
      log.info("Found {} position(s)", positionElements.size());

      // Validate required fields for each position
      for (int i = 0; i < positionElements.size(); i++) {
        Element position = positionElements.get(i);
        log.info("Validating position #{}", i + 1);

        // Validate required fields
        String positionId = XmlResponseValidator.getRequiredField(position, "positionId");
        String quantity = XmlResponseValidator.getRequiredField(position, "quantity");
        String positionType = XmlResponseValidator.getRequiredField(position, "positionType");
        String marketValue = XmlResponseValidator.getRequiredField(position, "marketValue");

        log.info("  Position ID: {}", positionId);
        log.info("  Quantity: {}", quantity);
        log.info("  Position Type: {}", positionType);
        log.info("  Market Value: {}", marketValue);

        // Validate Product element
        Element productElement = XmlResponseValidator.getFirstChildElement(position, "Product");
        assertNotNull(productElement, "Position should contain <Product> element");

        String symbol = XmlResponseValidator.getRequiredField(productElement, "symbol");
        String securityType = XmlResponseValidator.getRequiredField(productElement, "securityType");

        log.info("  Symbol: {}", symbol);
        log.info("  Security Type: {}", securityType);

        // Validate URLs if present
        String quoteDetailsUrl = XmlResponseValidator.getTextContent(position, "quoteDetails");
        if (quoteDetailsUrl != null && !quoteDetailsUrl.isEmpty()) {
          XmlResponseValidator.validateUrlField(position, "quoteDetails");
          log.info("  Quote Details URL: {}", quoteDetailsUrl);
        }

        String lotsDetailsUrl = XmlResponseValidator.getTextContent(position, "lotsDetails");
        if (lotsDetailsUrl != null && !lotsDetailsUrl.isEmpty()) {
          XmlResponseValidator.validateUrlField(position, "lotsDetails");
          log.info("  Lots Details URL: {}", lotsDetailsUrl);
        }
      }
    } else {
      log.info("No positions found in portfolio (account may be empty)");
    }

    log.info("✅ View Portfolio test passed - all required fields validated");
  }

  @Test
  @DisplayName("View Portfolio - Response Structure")
  void viewPortfolio_responseStructure() {
    log.info("=== Testing View Portfolio Response Structure ===");

    String endpoint = "/v1/accounts/" + accountIdKey + "/portfolio";
    String responseXml = apiClient.get(endpoint);
    Document doc = XmlResponseValidator.parseXml(responseXml);

    Element root = doc.getDocumentElement();
    assertEquals("PortfolioResponse", root.getNodeName(), "Root element should be PortfolioResponse");

    Element accountPortfolioElement = XmlResponseValidator.getFirstChildElement(root, "AccountPortfolio");
    assertNotNull(accountPortfolioElement, "Should have AccountPortfolio element");

    String accountId = XmlResponseValidator.getTextContent(accountPortfolioElement, "accountId");
    assertNotNull(accountId, "Should have accountId");

    log.info("✅ Response structure validated");
  }
}
