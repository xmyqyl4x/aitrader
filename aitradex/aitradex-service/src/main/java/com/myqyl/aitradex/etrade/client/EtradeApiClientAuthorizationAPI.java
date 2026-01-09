package com.myqyl.aitradex.etrade.client;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.repository.EtradeAuditLogRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * E*TRADE Authorization API Client.
 * 
 * This class refactors authorization-specific functionality from EtradeApiClient
 * and EtradeOAuthService into a dedicated Authorization API layer.
 * 
 * Implements all 5 Authorization API endpoints as per E*TRADE Authorization API documentation:
 * 1. Get Request Token
 * 2. Authorize Application (URL generation)
 * 3. Get Access Token
 * 4. Renew Access Token
 * 5. Revoke Access Token
 * 
 * All request and response objects are DTOs/Models, not Maps, as per requirements.
 */
@Component
public class EtradeApiClientAuthorizationAPI {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClientAuthorizationAPI.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final EtradeProperties properties;
  private final EtradeOAuth1Template oauthTemplate;
  private final EtradeAuditLogRepository auditLogRepository;
  private final HttpClient httpClient;

  public EtradeApiClientAuthorizationAPI(
      EtradeProperties properties,
      EtradeOAuth1Template oauthTemplate,
      EtradeAuditLogRepository auditLogRepository) {
    this.properties = properties;
    this.oauthTemplate = oauthTemplate;
    this.auditLogRepository = auditLogRepository;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
  }

  /**
   * 1. Get Request Token
   * 
   * Returns a temporary request token, initiating the OAuth process.
   * The request token expires after five minutes.
   * 
   * @param request The request DTO containing oauth_callback parameter
   * @return RequestTokenResponse DTO containing oauth_token, oauth_token_secret, oauth_callback_confirmed
   * @throws EtradeApiException if the request fails
   */
  public RequestTokenResponse getRequestToken(RequestTokenRequest request) {
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", "unauthorized");
    
    Instant startTime = Instant.now();
    String action = "GET " + properties.getOAuthRequestTokenUrl();
    
    try {
      String url = properties.getOAuthRequestTokenUrl();
      Map<String, String> params = new HashMap<>();
      params.put("oauth_callback", request.getOauthCallback());

      // Generate OAuth authorization header (no token for request token step)
      String authHeader = oauthTemplate.generateAuthorizationHeader("GET", url, params, null, null);

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();

      // Log to audit
      logAudit(null, action, params.toString(), null, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() != 200) {
        String errorMessage = "Failed to get request token: HTTP " + response.statusCode();
        log.error("Request token failed with status {}: {}", response.statusCode(), response.body());
        throw new EtradeApiException(response.statusCode(), "REQUEST_TOKEN_FAILED", errorMessage);
      }

      // Parse OAuth response (oauth_token, oauth_token_secret, oauth_callback_confirmed)
      Map<String, String> tokenParams = oauthTemplate.parseOAuthResponse(response.body());
      String oauthToken = tokenParams.get("oauth_token");
      String oauthTokenSecret = tokenParams.get("oauth_token_secret");
      String oauthCallbackConfirmed = tokenParams.getOrDefault("oauth_callback_confirmed", "false");

      if (oauthToken == null || oauthTokenSecret == null) {
        throw new EtradeApiException(400, "INVALID_RESPONSE", 
            "Invalid request token response: missing required fields");
      }

      log.info("Request token obtained successfully [correlationId={}]", correlationId);

      return new RequestTokenResponse(oauthToken, oauthTokenSecret, oauthCallbackConfirmed);
    } catch (EtradeApiException e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, e.getMessage(), durationMs);
      log.error("Get request token failed [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "Get request token failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  /**
   * 2. Authorize Application (Generate Authorization URL)
   * 
   * Generates the authorization URL to redirect the user to E*TRADE authorization page.
   * This is not a REST API call, but constructs the URL with request token and consumer key.
   * 
   * @param request The request DTO containing oauth_consumer_key and oauth_token
   * @return AuthorizeApplicationResponse DTO containing the authorization URL
   */
  public AuthorizeApplicationResponse authorizeApplication(AuthorizeApplicationRequest request) {
    try {
      // Build authorization URL: https://us.etrade.com/e/t/etws/authorize?key={consumerKey}&token={token}
      String authorizationUrl = properties.getAuthorizeUrl() + 
          "?key=" + request.getOauthConsumerKey() + 
          "&token=" + request.getOauthToken();
      
      log.info("Authorization URL generated for token: {}", maskToken(request.getOauthToken()));
      
      return new AuthorizeApplicationResponse(authorizationUrl);
    } catch (Exception e) {
      log.error("Failed to generate authorization URL", e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "Failed to generate authorization URL: " + e.getMessage(), e);
    }
  }

  /**
   * 3. Get Access Token
   * 
   * Exchanges request token + verifier for access token.
   * The production access token expires by default at midnight US Eastern time.
   * 
   * @param request The request DTO containing oauth_token, oauth_token_secret, and oauth_verifier
   * @return AccessTokenResponse DTO containing oauth_token and oauth_token_secret
   * @throws EtradeApiException if the request fails
   */
  public AccessTokenResponse getAccessToken(AccessTokenRequest request) {
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", "unauthorized");
    
    Instant startTime = Instant.now();
    String action = "GET " + properties.getOAuthAccessTokenUrl();
    
    try {
      String url = properties.getOAuthAccessTokenUrl();
      Map<String, String> params = new HashMap<>();
      params.put("oauth_verifier", request.getOauthVerifier());

      // Generate OAuth authorization header with request token
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          "GET", url, params, request.getOauthToken(), request.getOauthTokenSecret());

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();

      // Log to audit
      Map<String, String> auditParams = new HashMap<>();
      auditParams.put("oauth_verifier", maskToken(request.getOauthVerifier()));
      logAudit(null, action, auditParams.toString(), null, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() != 200) {
        String errorMessage = "Failed to exchange access token: HTTP " + response.statusCode();
        log.error("Access token exchange failed with status {}: {}", response.statusCode(), response.body());
        throw new EtradeApiException(response.statusCode(), "ACCESS_TOKEN_EXCHANGE_FAILED", errorMessage);
      }

      // Parse OAuth response (oauth_token, oauth_token_secret)
      Map<String, String> tokenParams = oauthTemplate.parseOAuthResponse(response.body());
      String accessToken = tokenParams.get("oauth_token");
      String accessTokenSecret = tokenParams.get("oauth_token_secret");

      if (accessToken == null || accessTokenSecret == null) {
        throw new EtradeApiException(400, "INVALID_RESPONSE", 
            "Invalid access token response: missing required fields");
      }

      log.info("Access token obtained successfully [correlationId={}]", correlationId);

      return new AccessTokenResponse(accessToken, accessTokenSecret);
    } catch (EtradeApiException e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, e.getMessage(), durationMs);
      log.error("Get access token failed [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "Get access token failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  /**
   * 4. Renew Access Token
   * 
   * Renews the OAuth access token after two hours or more of inactivity.
   * 
   * @param request The request DTO containing oauth_token and oauth_token_secret
   * @return RenewAccessTokenResponse DTO containing the renewal status message
   * @throws EtradeApiException if the request fails
   */
  public RenewAccessTokenResponse renewAccessToken(RenewAccessTokenRequest request) {
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", "token_renewal");
    
    Instant startTime = Instant.now();
    String action = "GET " + properties.getOAuthRenewAccessTokenUrl();
    
    try {
      String url = properties.getOAuthRenewAccessTokenUrl();
      Map<String, String> params = new HashMap<>();

      // Generate OAuth authorization header with access token
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          "GET", url, params, request.getOauthToken(), request.getOauthTokenSecret());

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();

      // Log to audit
      logAudit(null, action, params.toString(), null, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() != 200) {
        String errorMessage = "Failed to renew access token: HTTP " + response.statusCode();
        log.error("Renew access token failed with status {}: {}", response.statusCode(), response.body());
        throw new EtradeApiException(response.statusCode(), "RENEW_TOKEN_FAILED", errorMessage);
      }

      String message = response.body() != null ? response.body().trim() : "Access Token has been renewed";
      log.info("Access token renewed successfully [correlationId={}]", correlationId);

      return new RenewAccessTokenResponse(message);
    } catch (EtradeApiException e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, e.getMessage(), durationMs);
      log.error("Renew access token failed [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "Renew access token failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  /**
   * 5. Revoke Access Token
   * 
   * Revokes an access token that was granted for the consumer key.
   * Once revoked, it no longer grants access to E*TRADE data.
   * 
   * @param request The request DTO containing oauth_token and oauth_token_secret
   * @return RevokeAccessTokenResponse DTO containing the revocation status message
   * @throws EtradeApiException if the request fails
   */
  public RevokeAccessTokenResponse revokeAccessToken(RevokeAccessTokenRequest request) {
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", "token_revocation");
    
    Instant startTime = Instant.now();
    String action = "GET " + properties.getOAuthRevokeAccessTokenUrl();
    
    try {
      String url = properties.getOAuthRevokeAccessTokenUrl();
      Map<String, String> params = new HashMap<>();

      // Generate OAuth authorization header with access token
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          "GET", url, params, request.getOauthToken(), request.getOauthTokenSecret());

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .GET()
          .timeout(REQUEST_TIMEOUT)
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();

      // Log to audit
      logAudit(null, action, params.toString(), null, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() != 200) {
        String errorMessage = "Failed to revoke access token: HTTP " + response.statusCode();
        log.error("Revoke access token failed with status {}: {}", response.statusCode(), response.body());
        throw new EtradeApiException(response.statusCode(), "REVOKE_TOKEN_FAILED", errorMessage);
      }

      String message = response.body() != null ? response.body().trim() : "Revoked Access Token";
      
      // Parse response to extract token info if present
      Map<String, String> tokenParams = oauthTemplate.parseOAuthResponse(response.body());
      String oauthToken = tokenParams.get("oauth_token");
      String oauthTokenSecret = tokenParams.get("oauth_token_secret");

      log.info("Access token revoked successfully [correlationId={}]", correlationId);

      return new RevokeAccessTokenResponse(message, oauthToken, oauthTokenSecret);
    } catch (EtradeApiException e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, null, null, null, e.getMessage(), durationMs);
      log.error("Revoke access token failed [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "Revoke access token failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  /**
   * Logs audit entry for authorization API calls.
   */
  private void logAudit(UUID accountId, String action, String queryParams, String requestBody,
                       String responseBody, Integer statusCode, String errorMessage, long durationMs) {
    try {
      com.myqyl.aitradex.etrade.domain.EtradeAuditLog auditLog = 
          new com.myqyl.aitradex.etrade.domain.EtradeAuditLog();
      auditLog.setAccountId(accountId);
      // Truncate action if too long (max 100 chars per database schema)
      auditLog.setAction(action != null && action.length() > 100 ? 
          action.substring(0, 100) : action);
      auditLog.setRequestBody(requestBody != null ? 
          (requestBody.length() > 1000 ? requestBody.substring(0, 1000) + "..." : requestBody) : null);
      auditLog.setResponseBody(responseBody != null ? 
          (responseBody.length() > 5000 ? responseBody.substring(0, 5000) + "..." : responseBody) : null);
      auditLog.setStatusCode(statusCode);
      auditLog.setErrorMessage(errorMessage);
      auditLog.setDurationMs((int) durationMs);
      auditLog.setCreatedAt(java.time.OffsetDateTime.now());
      auditLogRepository.save(auditLog);
      
      String correlationId = MDC.get("correlationId");
      if (correlationId != null) {
        log.debug("Audit logged [correlationId={}, action={}, status={}, duration={}ms]", 
            correlationId, action, statusCode, durationMs);
      }
    } catch (Exception e) {
      log.warn("Failed to log audit entry", e);
    }
  }

  /**
   * Masks sensitive token information for logging.
   */
  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "***";
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }
}
