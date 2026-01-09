package com.myqyl.aitradex.etrade.oauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.myqyl.aitradex.etrade.client.EtradeApiClientAuthorizationAPI;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService.RequestTokenResponse;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone OAuth workflow tests that don't require Spring Boot context.
 * These tests validate Steps 1-2 (Request Token and Authorization URL) without database.
 * 
 * To run these tests:
 * 1. Docker is NOT required (these tests don't use database)
 * 2. Set environment variables: ETRADE_CONSUMER_KEY, ETRADE_CONSUMER_SECRET
 */
@DisplayName("E*TRADE OAuth Workflow Standalone Tests (No Database)")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ETRADE_CONSUMER_SECRET", matches = ".+")
class EtradeOAuthWorkflowStandaloneTest {

  private static final Logger log = LoggerFactory.getLogger(EtradeOAuthWorkflowStandaloneTest.class);
  
  private EtradeProperties properties;
  private EtradeOAuth1Template oauthTemplate;
  private EtradeTokenEncryption tokenEncryption;
  private EtradeOAuthService oauthService;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    
    // Create properties from environment variables
    properties = new EtradeProperties();
    String consumerKey = System.getenv("ETRADE_CONSUMER_KEY");
    String consumerSecret = System.getenv("ETRADE_CONSUMER_SECRET");
    String encryptionKey = System.getenv("ETRADE_ENCRYPTION_KEY");
    
    assumeTrue(consumerKey != null && !consumerKey.isEmpty(), "ETRADE_CONSUMER_KEY must be set");
    assumeTrue(consumerSecret != null && !consumerSecret.isEmpty(), "ETRADE_CONSUMER_SECRET must be set");
    
    properties.setConsumerKey(consumerKey);
    properties.setConsumerSecret(consumerSecret);
    properties.setBaseUrl(System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com"));
    properties.setAuthorizeUrl(System.getenv().getOrDefault("ETRADE_AUTHORIZE_URL", 
        "https://us.etrade.com/e/t/etws/authorize"));
    properties.setCallbackUrl(System.getenv().getOrDefault("ETRADE_CALLBACK_URL", 
        "http://localhost:4205/etrade-review-trade/callback"));
    
    // Set encryption key (use default if not provided)
    if (encryptionKey == null || encryptionKey.isEmpty()) {
      encryptionKey = EtradeTokenEncryption.generateKey();
      log.warn("Using generated encryption key for testing");
    }
    properties.setEncryptionKey(encryptionKey);
    
    // Create OAuth components
    oauthTemplate = new EtradeOAuth1Template(consumerKey, consumerSecret);
    tokenEncryption = new EtradeTokenEncryption(encryptionKey);
    
    // Create Authorization API client (with null audit repository for standalone use)
    EtradeApiClientAuthorizationAPI authorizationApi = new EtradeApiClientAuthorizationAPI(
        properties, oauthTemplate, null);
    
    // Create OAuth service with mocked repository (we won't use it for steps 1-2)
    oauthService = new EtradeOAuthService(properties, authorizationApi, tokenEncryption, null);
    
    log.info("Running OAuth workflow standalone tests against E*TRADE sandbox: {}", properties.getBaseUrl());
    log.info("Consumer Key: {}", maskKey(properties.getConsumerKey()));
  }

  @Test
  @DisplayName("Step 1: Request Token - Success (Standalone)")
  void step1_requestToken_success() {
    log.info("=== Step 1: Request Token (Standalone) ===");
    
    // Request token
    RequestTokenResponse response = oauthService.getRequestToken(testUserId);
    
    // Validate response
    assertNotNull(response, "Request token response should not be null");
    assertNotNull(response.getRequestToken(), "Request token should not be null");
    assertNotNull(response.getRequestTokenSecret(), "Request token secret should not be null");
    assertNotNull(response.getAuthorizationUrl(), "Authorization URL should not be null");
    
    // Validate authorization URL format
    String authUrl = response.getAuthorizationUrl();
    assertTrue(authUrl.contains("etrade.com"), "Authorization URL should contain etrade.com");
    assertTrue(authUrl.contains("authorize"), "Authorization URL should contain 'authorize'");
    assertTrue(authUrl.contains(properties.getConsumerKey()), 
        "Authorization URL should contain consumer key");
    assertTrue(authUrl.contains(response.getRequestToken()), 
        "Authorization URL should contain request token");
    
    log.info("✅ Request Token obtained:");
    log.info("  Request Token: {}", maskToken(response.getRequestToken()));
    log.info("  Request Token Secret: {}", maskToken(response.getRequestTokenSecret()));
    log.info("  Authorization URL: {}", authUrl);
    
    // Validate URL format: https://us.etrade.com/e/t/etws/authorize?key=<CONSUMER_KEY>&token=<OAUTH_TOKEN>
    try {
      URI uri = URI.create(authUrl);
      assertEquals("https", uri.getScheme(), "Authorization URL should use HTTPS");
      assertTrue(uri.getHost().contains("etrade.com"), "Authorization URL host should be etrade.com");
      
      String query = uri.getQuery();
      assertNotNull(query, "Authorization URL should have query parameters");
      assertTrue(query.contains("key="), "Authorization URL should have 'key' parameter");
      assertTrue(query.contains("token="), "Authorization URL should have 'token' parameter");
    } catch (Exception e) {
      fail("Invalid authorization URL format: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Step 1: Request Token - Validates OAuth Header Format (Standalone)")
  void step1_requestToken_oauthHeaderFormat() {
    log.info("=== Step 1: Request Token (OAuth Header Validation - Standalone) ===");
    
    RequestTokenResponse response = oauthService.getRequestToken(testUserId);
    
    // The OAuth header should be generated internally, but we can validate the request was made
    // by checking the response contains required OAuth parameters
    assertNotNull(response.getRequestToken(), "OAuth request should return oauth_token");
    assertNotNull(response.getRequestTokenSecret(), "OAuth request should return oauth_token_secret");
    
    // Validate token format (OAuth tokens are typically alphanumeric)
    assertTrue(response.getRequestToken().matches("[a-zA-Z0-9]+"), 
        "Request token should be alphanumeric");
    assertTrue(response.getRequestTokenSecret().matches("[a-zA-Z0-9]+"), 
        "Request token secret should be alphanumeric");
    
    log.info("✅ OAuth header generation validated (request token received)");
  }

  @Test
  @DisplayName("Step 2: Authorization URL - Format Validation (Standalone)")
  void step2_authorizationUrl_format() {
    log.info("=== Step 2: Authorization URL Generation (Standalone) ===");
    
    RequestTokenResponse response = oauthService.getRequestToken(testUserId);
    String authUrl = response.getAuthorizationUrl();
    
    // Parse and validate URL structure
    URI uri = URI.create(authUrl);
    String query = uri.getQuery();
    
    // Extract parameters
    Map<String, String> params = parseQueryString(query);
    
    // Validate required parameters
    assertTrue(params.containsKey("key"), "Authorization URL should have 'key' parameter");
    assertEquals(properties.getConsumerKey(), params.get("key"), 
        "Authorization URL 'key' should match consumer key");
    
    assertTrue(params.containsKey("token"), "Authorization URL should have 'token' parameter");
    assertEquals(response.getRequestToken(), params.get("token"), 
        "Authorization URL 'token' should match request token");
    
    log.info("✅ Authorization URL format validated:");
    log.info("  Base URL: {}", uri.getScheme() + "://" + uri.getHost() + uri.getPath());
    log.info("  Consumer Key (key): {}", maskKey(params.get("key")));
    log.info("  Request Token (token): {}", maskToken(params.get("token")));
    log.info("");
    log.info("⚠️  MANUAL STEP REQUIRED:");
    log.info("  1. Open this URL in your browser: {}", authUrl);
    log.info("  2. Log in to your E*TRADE sandbox account");
    log.info("  3. Authorize the application");
    log.info("  4. Copy the oauth_verifier from the callback URL or displayed page");
    log.info("  5. Use the verifier in Step 3 test (requires database/Spring Boot context)");
  }

  // Helper methods

  private String maskKey(String key) {
    if (key == null || key.length() <= 8) {
      return "***";
    }
    return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
  }

  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "***";
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }

  private Map<String, String> parseQueryString(String query) {
    Map<String, String> params = new java.util.HashMap<>();
    if (query != null && !query.isEmpty()) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          params.put(keyValue[0], keyValue[1]);
        }
      }
    }
    return params;
  }
}
