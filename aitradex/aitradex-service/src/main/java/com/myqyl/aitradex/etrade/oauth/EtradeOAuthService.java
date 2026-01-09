package com.myqyl.aitradex.etrade.oauth;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAuthorizationAPI;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing E*TRADE OAuth 1.0 flow.
 * 
 * This service now delegates to EtradeApiClientAuthorizationAPI for authorization API calls,
 * using proper DTOs/Models instead of Maps.
 */
@Service
public class EtradeOAuthService {

  private static final Logger log = LoggerFactory.getLogger(EtradeOAuthService.class);

  private final EtradeProperties properties;
  private final EtradeApiClientAuthorizationAPI authorizationApi;
  private final EtradeTokenEncryption tokenEncryption;
  private final EtradeOAuthTokenRepository tokenRepository;

  public EtradeOAuthService(
      EtradeProperties properties,
      EtradeApiClientAuthorizationAPI authorizationApi,
      EtradeTokenEncryption tokenEncryption,
      EtradeOAuthTokenRepository tokenRepository) {
    this.properties = properties;
    this.authorizationApi = authorizationApi;
    this.tokenEncryption = tokenEncryption;
    this.tokenRepository = tokenRepository;
  }

  /**
   * Step 1: Get request token and return authorization URL.
   * Returns both the URL and the request token data for storage.
   * 
   * This method now uses the Authorization API client with proper DTOs.
   */
  public RequestTokenResponse getRequestToken(UUID userId) {
    try {
      // For sandbox/testing, use "oob" (out-of-band) instead of callback URL
      // This allows manual verifier input for testing
      String callback = properties.getEnvironment() == EtradeProperties.Environment.SANDBOX 
          ? "oob" 
          : properties.getCallbackUrl();
      
      // Create request DTO
      RequestTokenRequest request = new RequestTokenRequest(callback);
      
      // Call Authorization API client
      com.myqyl.aitradex.etrade.authorization.dto.RequestTokenResponse apiResponse = 
          authorizationApi.getRequestToken(request);
      
      log.info("Request token obtained for user {}", userId);

      // Build authorization URL using Authorization API
      AuthorizeApplicationRequest authorizeRequest = new AuthorizeApplicationRequest(
          properties.getConsumerKey(), 
          apiResponse.getOauthToken());
      AuthorizeApplicationResponse authorizeResponse = authorizationApi.authorizeApplication(authorizeRequest);
      
      // Convert to legacy response format for backward compatibility
      return new RequestTokenResponse(
          authorizeResponse.getAuthorizationUrl(),
          apiResponse.getOauthToken(),
          apiResponse.getOauthTokenSecret());
    } catch (EtradeApiException e) {
      log.error("Failed to get request token", e);
      throw new RuntimeException("OAuth request token failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      log.error("Failed to get request token", e);
      throw new RuntimeException("OAuth request token failed", e);
    }
  }

  /**
   * Helper class for request token response (backward compatibility).
   * This wraps the DTO response and adds authorization URL for convenience.
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
   * 
   * This method now uses the Authorization API client with proper DTOs.
   * 
   * @return Map containing oauth_token and oauth_token_secret for backward compatibility
   */
  public Map<String, String> exchangeForAccessToken(String requestToken, String requestTokenSecret, 
                                                     String verifier, UUID accountId) {
    try {
      // Create request DTO
      AccessTokenRequest request = new AccessTokenRequest(requestToken, requestTokenSecret, verifier);
      
      // Call Authorization API client
      AccessTokenResponse apiResponse = authorizationApi.getAccessToken(request);
      
      // Store encrypted tokens
      EtradeOAuthToken token = new EtradeOAuthToken();
      token.setAccountId(accountId);
      token.setAccessTokenEncrypted(tokenEncryption.encrypt(apiResponse.getOauthToken()));
      token.setAccessTokenSecretEncrypted(tokenEncryption.encrypt(apiResponse.getOauthTokenSecret()));
      token.setRequestToken(requestToken);
      token.setRequestTokenSecret(requestTokenSecret);
      token.setOauthVerifier(verifier);

      tokenRepository.save(token);
      log.info("Access token stored for account {}", accountId);

      // Return Map for backward compatibility
      Map<String, String> tokenParams = new HashMap<>();
      tokenParams.put("oauth_token", apiResponse.getOauthToken());
      tokenParams.put("oauth_token_secret", apiResponse.getOauthTokenSecret());
      return tokenParams;
    } catch (EtradeApiException e) {
      log.error("Failed to exchange access token", e);
      throw new RuntimeException("Access token exchange failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      log.error("Failed to exchange access token", e);
      throw new RuntimeException("Access token exchange failed", e);
    }
  }

  /**
   * Renew access token after two hours or more of inactivity.
   * 
   * Uses the Authorization API client with proper DTOs.
   * 
   * @param accountId The account ID associated with the access token
   * @return RenewAccessTokenResponse DTO containing renewal status
   */
  public RenewAccessTokenResponse renewAccessToken(UUID accountId) {
    try {
      // Get access token for account
      AccessTokenPair tokenPair = getAccessToken(accountId);
      
      // Create request DTO
      RenewAccessTokenRequest request = new RenewAccessTokenRequest(
          tokenPair.getAccessToken(),
          tokenPair.getAccessTokenSecret());
      
      // Call Authorization API client
      return authorizationApi.renewAccessToken(request);
    } catch (EtradeApiException e) {
      log.error("Failed to renew access token for account {}", accountId, e);
      throw new RuntimeException("Renew access token failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      log.error("Failed to renew access token for account {}", accountId, e);
      throw new RuntimeException("Renew access token failed", e);
    }
  }

  /**
   * Revoke access token for an account.
   * 
   * Uses the Authorization API client with proper DTOs.
   * 
   * @param accountId The account ID associated with the access token
   * @return RevokeAccessTokenResponse DTO containing revocation status
   */
  public RevokeAccessTokenResponse revokeAccessToken(UUID accountId) {
    try {
      // Get access token for account
      AccessTokenPair tokenPair = getAccessToken(accountId);
      
      // Create request DTO
      RevokeAccessTokenRequest request = new RevokeAccessTokenRequest(
          tokenPair.getAccessToken(),
          tokenPair.getAccessTokenSecret());
      
      // Call Authorization API client
      RevokeAccessTokenResponse response = authorizationApi.revokeAccessToken(request);
      
      // Delete token from database after successful revocation
      tokenRepository.deleteByAccountId(accountId);
      log.info("Access token revoked and deleted for account {}", accountId);
      
      return response;
    } catch (EtradeApiException e) {
      log.error("Failed to revoke access token for account {}", accountId, e);
      throw new RuntimeException("Revoke access token failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      log.error("Failed to revoke access token for account {}", accountId, e);
      throw new RuntimeException("Revoke access token failed", e);
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
