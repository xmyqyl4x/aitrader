package com.myqyl.aitradex.etrade.client;

import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.oauth.EtradeTokenService;
import com.myqyl.aitradex.etrade.repository.EtradeAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Base API client for E*TRADE API calls with OAuth 1.0 signing.
 */
@Component
public class EtradeApiClient {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClient.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_RETRIES = 3;

  private final EtradeOAuth1Template oauthTemplate;
  private final EtradeTokenService tokenService;
  private final EtradeAuditLogRepository auditLogRepository;
  private final HttpClient httpClient;

  public EtradeApiClient(
      EtradeOAuth1Template oauthTemplate,
      EtradeTokenService tokenService,
      EtradeAuditLogRepository auditLogRepository,
      ObjectMapper objectMapper) {
    this.oauthTemplate = oauthTemplate;
    this.tokenService = tokenService;
    this.auditLogRepository = auditLogRepository;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
  }

  /**
   * Makes an authenticated OAuth 1.0 request to E*TRADE API.
   */
  public String makeRequest(String method, String url, Map<String, String> queryParams, 
                           String requestBody, UUID accountId) {
    return makeRequestWithRetry(method, url, queryParams, requestBody, accountId, 0);
  }

  private String makeRequestWithRetry(String method, String url, Map<String, String> queryParams,
                                     String requestBody, UUID accountId, int retryCount) {
    Instant startTime = Instant.now();
    String action = method + " " + url;
    String auditRequestBody = requestBody;
    
    try {
      // Get access token for account
      Optional<EtradeTokenService.AccessTokenPair> tokenOpt = tokenService.getAccessToken(accountId);
      if (tokenOpt.isEmpty()) {
        throw new RuntimeException("No access token found for account " + accountId);
      }
      EtradeTokenService.AccessTokenPair tokens = tokenOpt.get();

      // Build full URL with query params
      String fullUrl = url;
      if (queryParams != null && !queryParams.isEmpty()) {
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        queryParams.forEach((k, v) -> urlBuilder.append(k).append("=").append(v != null ? v : "").append("&"));
        fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);
      }

      // Generate OAuth authorization header
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          method, fullUrl, queryParams, tokens.getAccessToken(), tokens.getAccessTokenSecret());

      // Build HTTP request
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(fullUrl))
          .header("Authorization", authHeader)
          .header("Accept", "application/json")
          .timeout(REQUEST_TIMEOUT);

      if (requestBody != null && !requestBody.isEmpty()) {
        requestBuilder.header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(requestBody));
      } else {
        requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
      }

      HttpRequest request = requestBuilder.build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();

      // Handle rate limiting
      if (response.statusCode() == 429 && retryCount < MAX_RETRIES) {
        int waitSeconds = (int) Math.pow(2, retryCount) * 5; // Exponential backoff: 5s, 10s, 20s
        log.warn("Rate limited, retrying in {} seconds (attempt {}/{})", waitSeconds, retryCount + 1, MAX_RETRIES);
        Thread.sleep(waitSeconds * 1000L);
        return makeRequestWithRetry(method, url, queryParams, requestBody, accountId, retryCount + 1);
      }

      // Log to audit
      logAudit(accountId, action, queryParams != null ? queryParams.toString() : null, 
               auditRequestBody, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() >= 400) {
        log.error("E*TRADE API error {}: {}", response.statusCode(), response.body());
        throw new RuntimeException("E*TRADE API error: " + response.statusCode() + " - " + response.body());
      }

      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(accountId, action, null, auditRequestBody, null, null, e.getMessage(), durationMs);
      log.error("E*TRADE API request failed", e);
      throw new RuntimeException("E*TRADE API request failed", e);
    }
  }

  private void logAudit(UUID accountId, String action, String queryParams, String requestBody,
                       String responseBody, Integer statusCode, String errorMessage, long durationMs) {
    try {
      com.myqyl.aitradex.etrade.domain.EtradeAuditLog auditLog = 
          new com.myqyl.aitradex.etrade.domain.EtradeAuditLog();
      auditLog.setAccountId(accountId);
      auditLog.setAction(action);
      auditLog.setRequestBody(requestBody != null ? requestBody.substring(0, Math.min(requestBody.length(), 1000)) : queryParams);
      auditLog.setResponseBody(responseBody != null ? responseBody.substring(0, Math.min(responseBody.length(), 5000)) : null);
      auditLog.setStatusCode(statusCode);
      auditLog.setErrorMessage(errorMessage);
      auditLog.setDurationMs((int) durationMs);
      auditLogRepository.save(auditLog);
    } catch (Exception e) {
      log.warn("Failed to log audit entry", e);
    }
  }
}
