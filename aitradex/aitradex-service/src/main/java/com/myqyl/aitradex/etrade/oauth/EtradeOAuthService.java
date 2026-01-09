package com.myqyl.aitradex.etrade.oauth;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAuthorizationAPI;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
   * Creates and persists an authorization attempt record with status PENDING.
   * 
   * This method now uses the Authorization API client with proper DTOs.
   * 
   * @param userId The user ID initiating the authorization
   * @param correlationId Optional correlation ID for tracking (generated if not provided)
   * @return RequestTokenResponse containing authorization URL and request token data
   */
  public RequestTokenResponse getRequestToken(UUID userId, String correlationId) {
    OffsetDateTime startTime = OffsetDateTime.now();
    
    // Generate correlation ID if not provided
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }
    
    // Set correlation ID in MDC for logging
    MDC.put("correlationId", correlationId);
    MDC.put("userId", userId != null ? userId.toString() : "anonymous");
    
    // Create authorization attempt record (PENDING status)
    EtradeOAuthToken authAttempt = new EtradeOAuthToken();
    authAttempt.setUserId(userId);
    authAttempt.setStatus("PENDING");
    authAttempt.setStartTime(startTime);
    authAttempt.setEnvironment(properties.getEnvironment().name());
    authAttempt.setCorrelationId(correlationId);
    
    // Persist authorization attempt BEFORE making API call (so we have a record even if API call fails)
    // This ensures we track every attempt, successful or not
    UUID authAttemptId = null;
    try {
      authAttempt = tokenRepository.save(authAttempt);
      authAttemptId = authAttempt.getId();
      log.debug("Authorization attempt created with ID: {} (correlationId: {})", authAttemptId, correlationId);
    } catch (Exception saveException) {
      log.error("Failed to save initial authorization attempt (correlationId: {})", correlationId, saveException);
      // Continue anyway - we'll try to save again in catch block if API call fails
    }
    
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
      
      // Update authorization attempt with request token data
      authAttempt.setRequestToken(apiResponse.getOauthToken());
      authAttempt.setRequestTokenSecret(apiResponse.getOauthTokenSecret());
      
      // Update authorization attempt (still PENDING until access token exchange)
      tokenRepository.save(authAttempt);
      
      log.info("Request token obtained for user {} (correlationId: {})", userId, correlationId);

      // Build authorization URL using Authorization API
      AuthorizeApplicationRequest authorizeRequest = new AuthorizeApplicationRequest(
          properties.getConsumerKey(), 
          apiResponse.getOauthToken());
      AuthorizeApplicationResponse authorizeResponse = authorizationApi.authorizeApplication(authorizeRequest);
      
      // Convert to legacy response format for backward compatibility
      RequestTokenResponse response = new RequestTokenResponse(
          authorizeResponse.getAuthorizationUrl(),
          apiResponse.getOauthToken(),
          apiResponse.getOauthTokenSecret(),
          correlationId,
          authAttemptId != null ? authAttemptId : authAttempt.getId());
      
      return response;
    } catch (EtradeApiException e) {
      // Update authorization attempt with failure
      try {
        if (authAttemptId != null || authAttempt.getId() != null) {
          // Update existing attempt
          authAttempt.setStatus("FAILED");
          authAttempt.setEndTime(OffsetDateTime.now());
          authAttempt.setErrorMessage(e.getErrorMessage());
          authAttempt.setErrorCode(e.getErrorCode());
          tokenRepository.save(authAttempt);
          log.info("Authorization attempt marked as FAILED (ID: {}, correlationId: {})", authAttempt.getId(), correlationId);
        } else {
          // Try to save new failed attempt
          authAttempt.setStatus("FAILED");
          authAttempt.setEndTime(OffsetDateTime.now());
          authAttempt.setErrorMessage(e.getErrorMessage());
          authAttempt.setErrorCode(e.getErrorCode());
          tokenRepository.save(authAttempt);
          log.info("Authorization attempt saved as FAILED (ID: {}, correlationId: {})", authAttempt.getId(), correlationId);
        }
      } catch (Exception saveException) {
        log.error("Failed to save failed authorization attempt (correlationId: {})", correlationId, saveException);
      }
      
      log.error("Failed to get request token (correlationId: {})", correlationId, e);
      throw new RuntimeException("OAuth request token failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      // Update authorization attempt with failure
      try {
        if (authAttemptId != null || authAttempt.getId() != null) {
          // Update existing attempt
          authAttempt.setStatus("FAILED");
          authAttempt.setEndTime(OffsetDateTime.now());
          authAttempt.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
          authAttempt.setErrorCode("EXCEPTION");
          tokenRepository.save(authAttempt);
          log.info("Authorization attempt marked as FAILED (ID: {}, correlationId: {})", authAttempt.getId(), correlationId);
        } else {
          // Try to save new failed attempt
          authAttempt.setStatus("FAILED");
          authAttempt.setEndTime(OffsetDateTime.now());
          authAttempt.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
          authAttempt.setErrorCode("EXCEPTION");
          tokenRepository.save(authAttempt);
          log.info("Authorization attempt saved as FAILED (ID: {}, correlationId: {})", authAttempt.getId(), correlationId);
        }
      } catch (Exception saveException) {
        log.error("Failed to save failed authorization attempt (correlationId: {})", correlationId, saveException);
      }
      
      log.error("Failed to get request token (correlationId: {})", correlationId, e);
      throw new RuntimeException("OAuth request token failed", e);
    } finally {
      MDC.clear();
    }
  }
  
  /**
   * Step 1: Get request token and return authorization URL (overload without correlation ID).
   */
  public RequestTokenResponse getRequestToken(UUID userId) {
    return getRequestToken(userId, null);
  }

  /**
   * Helper class for request token response (backward compatibility).
   * This wraps the DTO response and adds authorization URL for convenience.
   */
  public static class RequestTokenResponse {
    private final String authorizationUrl;
    private final String requestToken;
    private final String requestTokenSecret;
    private final String correlationId;
    private final UUID authAttemptId;

    public RequestTokenResponse(String authorizationUrl, String requestToken, String requestTokenSecret) {
      this(authorizationUrl, requestToken, requestTokenSecret, null, null);
    }

    public RequestTokenResponse(String authorizationUrl, String requestToken, String requestTokenSecret,
                                String correlationId, UUID authAttemptId) {
      this.authorizationUrl = authorizationUrl;
      this.requestToken = requestToken;
      this.requestTokenSecret = requestTokenSecret;
      this.correlationId = correlationId;
      this.authAttemptId = authAttemptId;
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

    public String getCorrelationId() {
      return correlationId;
    }

    public UUID getAuthAttemptId() {
      return authAttemptId;
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
   * Updates the existing authorization attempt record with SUCCESS or FAILED status.
   * 
   * This method now uses the Authorization API client with proper DTOs.
   * 
   * @param requestToken The request token from Step 1
   * @param requestTokenSecret The request token secret from Step 1
   * @param verifier The oauth_verifier from user authorization
   * @param accountId The account ID to associate with the access token (null allowed initially)
   * @return Map containing oauth_token and oauth_token_secret for backward compatibility
   */
  public Map<String, String> exchangeForAccessToken(String requestToken, String requestTokenSecret, 
                                                     String verifier, UUID accountId) {
    OffsetDateTime endTime = OffsetDateTime.now();
    
    // Find existing authorization attempt by request token
    Optional<EtradeOAuthToken> existingAttempt = tokenRepository.findByRequestToken(requestToken);
    EtradeOAuthToken authAttempt;
    
    if (existingAttempt.isPresent()) {
      // Update existing authorization attempt
      authAttempt = existingAttempt.get();
      authAttempt.setOauthVerifier(verifier);
      authAttempt.setEndTime(endTime);
    } else {
      // Create new authorization attempt if not found (shouldn't happen, but handle gracefully)
      log.warn("Authorization attempt not found for request token {}, creating new record", 
               maskToken(requestToken));
      authAttempt = new EtradeOAuthToken();
      authAttempt.setRequestToken(requestToken);
      authAttempt.setRequestTokenSecret(requestTokenSecret);
      authAttempt.setOauthVerifier(verifier);
      authAttempt.setStartTime(OffsetDateTime.now());
      authAttempt.setEndTime(endTime);
      authAttempt.setStatus("PENDING");
      authAttempt.setEnvironment(properties.getEnvironment().name());
      authAttempt.setCorrelationId(UUID.randomUUID().toString());
    }
    
    // Set correlation ID in MDC for logging
    if (authAttempt.getCorrelationId() != null) {
      MDC.put("correlationId", authAttempt.getCorrelationId());
    }
    if (accountId != null) {
      MDC.put("accountId", accountId.toString());
    }
    
    try {
      // Create request DTO
      AccessTokenRequest request = new AccessTokenRequest(requestToken, requestTokenSecret, verifier);
      
      // Call Authorization API client
      AccessTokenResponse apiResponse = authorizationApi.getAccessToken(request);
      
      // Update authorization attempt with success status and access token data
      authAttempt.setStatus("SUCCESS");
      authAttempt.setEndTime(endTime);
      authAttempt.setAccountId(accountId);
      authAttempt.setAccessTokenEncrypted(tokenEncryption.encrypt(apiResponse.getOauthToken()));
      authAttempt.setAccessTokenSecretEncrypted(tokenEncryption.encrypt(apiResponse.getOauthTokenSecret()));
      authAttempt.setOauthVerifier(verifier);
      
      // Calculate access token expiry (production tokens expire at midnight US Eastern time)
      // For now, set to 24 hours from now for simplicity
      // In production, this should be calculated based on E*TRADE's token expiry rules
      OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
      authAttempt.setExpiresAt(expiresAt);

      tokenRepository.save(authAttempt);
      log.info("Access token stored for account {} (correlationId: {})", accountId, 
               authAttempt.getCorrelationId());

      // Return Map for backward compatibility
      Map<String, String> tokenParams = new HashMap<>();
      tokenParams.put("oauth_token", apiResponse.getOauthToken());
      tokenParams.put("oauth_token_secret", apiResponse.getOauthTokenSecret());
      return tokenParams;
    } catch (EtradeApiException e) {
      // Update authorization attempt with failure
      authAttempt.setStatus("FAILED");
      authAttempt.setEndTime(endTime);
      authAttempt.setErrorMessage(e.getErrorMessage());
      authAttempt.setErrorCode(e.getErrorCode());
      tokenRepository.save(authAttempt);
      
      log.error("Failed to exchange access token (correlationId: {})", 
               authAttempt.getCorrelationId(), e);
      throw new RuntimeException("Access token exchange failed: " + e.getErrorMessage(), e);
    } catch (Exception e) {
      // Update authorization attempt with failure
      authAttempt.setStatus("FAILED");
      authAttempt.setEndTime(endTime);
      authAttempt.setErrorMessage(e.getMessage());
      authAttempt.setErrorCode("EXCEPTION");
      tokenRepository.save(authAttempt);
      
      log.error("Failed to exchange access token (correlationId: {})", 
               authAttempt.getCorrelationId(), e);
      throw new RuntimeException("Access token exchange failed", e);
    } finally {
      MDC.clear();
    }
  }
  
  /**
   * Helper method to mask sensitive token values in logs.
   */
  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "***";
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
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
