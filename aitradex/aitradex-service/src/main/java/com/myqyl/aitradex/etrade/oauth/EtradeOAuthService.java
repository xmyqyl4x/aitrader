package com.myqyl.aitradex.etrade.oauth;

import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing E*TRADE OAuth 1.0 flow.
 */
@Service
public class EtradeOAuthService {

  private static final Logger log = LoggerFactory.getLogger(EtradeOAuthService.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final EtradeProperties properties;
  private final EtradeOAuth1Template oauthTemplate;
  private final EtradeTokenEncryption tokenEncryption;
  private final EtradeOAuthTokenRepository tokenRepository;
  private final HttpClient httpClient;

  public EtradeOAuthService(
      EtradeProperties properties,
      EtradeOAuth1Template oauthTemplate,
      EtradeTokenEncryption tokenEncryption,
      EtradeOAuthTokenRepository tokenRepository) {
    this.properties = properties;
    this.oauthTemplate = oauthTemplate;
    this.tokenEncryption = tokenEncryption;
    this.tokenRepository = tokenRepository;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
  }

  /**
   * Step 1: Get request token and return authorization URL.
   * Returns both the URL and the request token data for storage.
   */
  public RequestTokenResponse getRequestToken(UUID userId) {
    try {
      String url = properties.getOAuthRequestTokenUrl();
      Map<String, String> params = new HashMap<>();
      params.put("oauth_callback", properties.getCallbackUrl());

      // E*TRADE uses GET for request token (matching example app)
      String authHeader = oauthTemplate.generateAuthorizationHeader("GET", url, params, null, null);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("Request token failed with status {}: {}", response.statusCode(), response.body());
        throw new RuntimeException("Failed to get request token: " + response.statusCode());
      }

      Map<String, String> tokenParams = oauthTemplate.parseOAuthResponse(response.body());
      String requestToken = tokenParams.get("oauth_token");
      String requestTokenSecret = tokenParams.get("oauth_token_secret");

      if (requestToken == null || requestTokenSecret == null) {
        throw new RuntimeException("Invalid request token response");
      }

      log.info("Request token obtained for user {}", userId);

      // Build authorization URL
      String authorizationUrl = properties.getAuthorizeUrl() + "?key=" + properties.getConsumerKey() + "&token=" + requestToken;
      
      return new RequestTokenResponse(authorizationUrl, requestToken, requestTokenSecret);
    } catch (Exception e) {
      log.error("Failed to get request token", e);
      throw new RuntimeException("OAuth request token failed", e);
    }
  }

  /**
   * Helper class for request token response.
   */
  public static class RequestTokenResponse {
    private final String authorizationUrl;
    private final String requestToken;
    private final String requestTokenSecret;

    public RequestTokenResponse(String authorizationUrl, String requestToken, String requestTokenSecret) {
      this.authorizationUrl = authorizationUrl;
      this.requestToken = requestToken;
      this.requestTokenSecret = requestTokenSecret;
    }

    public String getAuthorizationUrl() {
      return authorizationUrl;
    }

    public String getRequestToken() {
      return requestToken;
    }

    public String getRequestTokenSecret() {
      return requestTokenSecret;
    }
  }

  /**
   * Step 1 (legacy): Get request token and return authorization URL.
   * @deprecated Use getRequestToken instead
   */
  @Deprecated
  public String getAuthorizationUrl(UUID userId) {
    return getRequestToken(userId).getAuthorizationUrl();
  }

  /**
   * Step 2: Exchange request token + verifier for access token.
   */
  public Map<String, String> exchangeForAccessToken(String requestToken, String requestTokenSecret, 
                                                     String verifier, UUID accountId) {
    try {
      String url = properties.getOAuthAccessTokenUrl();
      Map<String, String> params = new HashMap<>();
      params.put("oauth_verifier", verifier);

      // E*TRADE uses GET for access token (matching example app)
      String authHeader = oauthTemplate.generateAuthorizationHeader("GET", url, params, 
                                                                   requestToken, requestTokenSecret);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("Access token exchange failed with status {}: {}", response.statusCode(), response.body());
        throw new RuntimeException("Failed to exchange access token: " + response.statusCode());
      }

      Map<String, String> tokenParams = oauthTemplate.parseOAuthResponse(response.body());
      String accessToken = tokenParams.get("oauth_token");
      String accessTokenSecret = tokenParams.get("oauth_token_secret");

      if (accessToken == null || accessTokenSecret == null) {
        throw new RuntimeException("Invalid access token response");
      }

      // Store encrypted tokens
      EtradeOAuthToken token = new EtradeOAuthToken();
      token.setAccountId(accountId);
      token.setAccessTokenEncrypted(tokenEncryption.encrypt(accessToken));
      token.setAccessTokenSecretEncrypted(tokenEncryption.encrypt(accessTokenSecret));
      token.setRequestToken(requestToken);
      token.setRequestTokenSecret(requestTokenSecret);
      token.setOauthVerifier(verifier);

      tokenRepository.save(token);
      log.info("Access token stored for account {}", accountId);

      return tokenParams;
    } catch (Exception e) {
      log.error("Failed to exchange access token", e);
      throw new RuntimeException("Access token exchange failed", e);
    }
  }

  /**
   * Retrieves access token for an account (decrypted).
   */
  public AccessTokenPair getAccessToken(UUID accountId) {
    return tokenRepository.findByAccountId(accountId)
        .map(token -> new AccessTokenPair(
            tokenEncryption.decrypt(token.getAccessTokenEncrypted()),
            tokenEncryption.decrypt(token.getAccessTokenSecretEncrypted())))
        .orElseThrow(() -> new RuntimeException("No access token found for account " + accountId));
  }

  /**
   * Gets account list from E*TRADE (used after OAuth to link accounts).
   */
  public List<Map<String, Object>> getAccountList(String accessToken, String accessTokenSecret) {
    // This will be implemented when we have the account client
    // For now, return empty list
    return List.of();
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
