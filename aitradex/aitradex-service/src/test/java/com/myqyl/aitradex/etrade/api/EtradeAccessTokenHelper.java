package com.myqyl.aitradex.etrade.api;

import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuthService;
import com.myqyl.aitradex.etrade.oauth.EtradeTokenEncryption;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility to obtain E*TRADE access token for API testing.
 * This can be used to get an access token programmatically for running API tests.
 */
public class EtradeAccessTokenHelper {

  private static final Logger log = LoggerFactory.getLogger(EtradeAccessTokenHelper.class);

  /**
   * Gets an access token using the OAuth workflow.
   * 
   * @param consumerKey E*TRADE consumer key
   * @param consumerSecret E*TRADE consumer secret
   * @param verifier OAuth verifier from authorization step
   * @return AccessTokenPair containing access token and secret
   */
  public static AccessTokenPair getAccessToken(String consumerKey, String consumerSecret, String verifier) {
    try {
      // Create properties
      EtradeProperties properties = new EtradeProperties();
      properties.setConsumerKey(consumerKey);
      properties.setConsumerSecret(consumerSecret);
      properties.setBaseUrl(System.getenv().getOrDefault("ETRADE_BASE_URL", "https://apisb.etrade.com"));
      properties.setAuthorizeUrl(System.getenv().getOrDefault("ETRADE_AUTHORIZE_URL", 
          "https://us.etrade.com/e/t/etws/authorize"));
      properties.setCallbackUrl(System.getenv().getOrDefault("ETRADE_CALLBACK_URL", 
          "http://localhost:4200/etrade-review-trade/callback"));
      properties.setEnvironment(EtradeProperties.Environment.SANDBOX);
      
      String encryptionKey = System.getenv("ETRADE_ENCRYPTION_KEY");
      if (encryptionKey == null || encryptionKey.isEmpty()) {
        encryptionKey = EtradeTokenEncryption.generateKey();
      }
      properties.setEncryptionKey(encryptionKey);

      // Create OAuth components
      EtradeOAuth1Template oauthTemplate = new EtradeOAuth1Template(consumerKey, consumerSecret);
      EtradeTokenEncryption tokenEncryption = new EtradeTokenEncryption(encryptionKey);
      
      // Create OAuth service (with null repository - exchangeForAccessToken will fail on save, 
      // but we can extract the token from the response before that)
      // Actually, we need to catch the exception or modify approach
      // For now, let's use a mock repository approach or catch the save exception
      EtradeOAuthService oauthService = new EtradeOAuthService(
          properties, oauthTemplate, tokenEncryption, null);

      // Step 1: Get request token
      UUID testUserId = UUID.randomUUID();
      EtradeOAuthService.RequestTokenResponse requestTokenResponse = 
          oauthService.getRequestToken(testUserId);
      
      String requestToken = requestTokenResponse.getRequestToken();
      String requestTokenSecret = requestTokenResponse.getRequestTokenSecret();
      
      log.info("Request token obtained");

      // Step 2: Exchange for access token (using provided verifier)
      // We'll make the API call directly since exchangeForAccessToken tries to save to DB
      // Let's replicate the exchange logic without DB save
      String url = properties.getOAuthAccessTokenUrl();
      Map<String, String> params = new java.util.HashMap<>();
      params.put("oauth_verifier", verifier);

      // Generate OAuth header
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          "GET", url, params, requestToken, requestTokenSecret);

      // Make HTTP request
      java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
          .connectTimeout(java.time.Duration.ofSeconds(30))
          .build();
      
      java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(java.time.Duration.ofSeconds(30))
          .build();

      java.net.http.HttpResponse<String> response = 
          httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to exchange access token: " + response.statusCode() + " - " + response.body());
      }

      // Parse response (replicate parseOAuthResponse logic)
      Map<String, String> accessTokenResponse = parseOAuthResponse(response.body());
      String accessToken = accessTokenResponse.get("oauth_token");
      String accessTokenSecret = accessTokenResponse.get("oauth_token_secret");

      if (accessToken == null || accessTokenSecret == null) {
        throw new RuntimeException("Failed to obtain access token from response");
      }

      log.info("Access token obtained successfully");
      return new AccessTokenPair(accessToken, accessTokenSecret);
    } catch (Exception e) {
      log.error("Failed to get access token", e);
      throw new RuntimeException("Failed to get access token", e);
    }
  }

  /**
   * Parses OAuth response (token and secret).
   */
  private static Map<String, String> parseOAuthResponse(String responseBody) {
    Map<String, String> params = new java.util.HashMap<>();
    if (responseBody != null && !responseBody.isEmpty()) {
      String[] pairs = responseBody.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          params.put(keyValue[0], decode(keyValue[1]));
        }
      }
    }
    return params;
  }

  private static String decode(String value) {
    try {
      return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      return value;
    }
  }

  /**
   * Access token pair.
   */
  public static class AccessTokenPair {
    private final String accessToken;
    private final String accessTokenSecret;

    public AccessTokenPair(String accessToken, String accessTokenSecret) {
      this.accessToken = accessToken;
      this.accessTokenSecret = accessTokenSecret;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public String getAccessTokenSecret() {
      return accessTokenSecret;
    }
  }
}
