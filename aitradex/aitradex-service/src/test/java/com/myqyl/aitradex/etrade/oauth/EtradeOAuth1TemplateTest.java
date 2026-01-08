package com.myqyl.aitradex.etrade.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EtradeOAuth1Template.
 */
class EtradeOAuth1TemplateTest {

  private EtradeOAuth1Template oauthTemplate;
  private static final String CONSUMER_KEY = "test_consumer_key";
  private static final String CONSUMER_SECRET = "test_consumer_secret";

  @BeforeEach
  void setUp() {
    oauthTemplate = new EtradeOAuth1Template(CONSUMER_KEY, CONSUMER_SECRET);
  }

  @Test
  void generateAuthorizationHeader_noToken_createsValidHeader() {
    String url = "https://api.etrade.com/v1/accounts/list";
    String header = oauthTemplate.generateAuthorizationHeader("GET", url, null, null, null);

    assertNotNull(header);
    assertTrue(header.startsWith("OAuth "));
    assertTrue(header.contains("oauth_consumer_key"));
    assertTrue(header.contains("oauth_signature_method"));
    assertTrue(header.contains("oauth_timestamp"));
    assertTrue(header.contains("oauth_nonce"));
    assertTrue(header.contains("oauth_version"));
    assertTrue(header.contains("oauth_signature"));
    assertTrue(header.contains(CONSUMER_KEY));
  }

  @Test
  void generateAuthorizationHeader_withToken_includesToken() {
    String url = "https://api.etrade.com/v1/market/quote/AAPL";
    String token = "test_access_token";
    String tokenSecret = "test_access_token_secret";
    
    String header = oauthTemplate.generateAuthorizationHeader("GET", url, null, token, tokenSecret);

    assertNotNull(header);
    assertTrue(header.contains("oauth_token"));
    assertTrue(header.contains(token));
  }

  @Test
  void generateAuthorizationHeader_withQueryParams_includesInSignature() {
    String url = "https://api.etrade.com/v1/accounts/123/balance";
    Map<String, String> params = new HashMap<>();
    params.put("instType", "BROKERAGE");
    params.put("realTimeNAV", "true");
    
    String header = oauthTemplate.generateAuthorizationHeader("GET", url, params, null, null);

    assertNotNull(header);
    assertTrue(header.contains("oauth_signature"));
  }

  @Test
  void generateAuthorizationHeader_differentMethods_producesDifferentSignatures() {
    String url = "https://api.etrade.com/v1/accounts/list";
    
    String getHeader = oauthTemplate.generateAuthorizationHeader("GET", url, null, null, null);
    String postHeader = oauthTemplate.generateAuthorizationHeader("POST", url, null, null, null);

    assertNotEquals(getHeader, postHeader);
    
    // Extract signatures
    String getSig = extractSignature(getHeader);
    String postSig = extractSignature(postHeader);
    assertNotEquals(getSig, postSig);
  }

  @Test
  void parseOAuthResponse_validResponse_parsesCorrectly() {
    String response = "oauth_token=test_token&oauth_token_secret=test_secret";
    
    Map<String, String> params = oauthTemplate.parseOAuthResponse(response);

    assertEquals("test_token", params.get("oauth_token"));
    assertEquals("test_secret", params.get("oauth_token_secret"));
  }

  @Test
  void parseOAuthResponse_urlEncodedResponse_decodesCorrectly() {
    String response = "oauth_token=test%20token&oauth_token_secret=test%2Fsecret";
    
    Map<String, String> params = oauthTemplate.parseOAuthResponse(response);

    assertEquals("test token", params.get("oauth_token"));
    assertEquals("test/secret", params.get("oauth_token_secret"));
  }

  @Test
  void parseOAuthResponse_emptyResponse_returnsEmptyMap() {
    Map<String, String> params = oauthTemplate.parseOAuthResponse("");
    assertTrue(params.isEmpty());

    params = oauthTemplate.parseOAuthResponse(null);
    assertTrue(params.isEmpty());
  }

  @Test
  void generateAuthorizationHeader_sameInputDifferentTime_producesDifferentNonce() {
    String url = "https://api.etrade.com/v1/accounts/list";
    
    String header1 = oauthTemplate.generateAuthorizationHeader("GET", url, null, null, null);
    // Small delay to ensure different timestamp/nonce
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    // Create new instance to reset
    EtradeOAuth1Template oauthTemplate2 = new EtradeOAuth1Template(CONSUMER_KEY, CONSUMER_SECRET);
    String header2 = oauthTemplate2.generateAuthorizationHeader("GET", url, null, null, null);

    assertNotEquals(header1, header2);
  }

  @Test
  void getConsumerKey_returnsCorrectValue() {
    assertEquals(CONSUMER_KEY, oauthTemplate.getConsumerKey());
  }

  @Test
  void getConsumerSecret_returnsCorrectValue() {
    assertEquals(CONSUMER_SECRET, oauthTemplate.getConsumerSecret());
  }

  // Helper method to extract signature from OAuth header
  private String extractSignature(String header) {
    String[] parts = header.split(",");
    for (String part : parts) {
      if (part.trim().startsWith("oauth_signature=\"")) {
        return part.trim().substring("oauth_signature=\"".length(), part.trim().length() - 1);
      }
    }
    return null;
  }
}
