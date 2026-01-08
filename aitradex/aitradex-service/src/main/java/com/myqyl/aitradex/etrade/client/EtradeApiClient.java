package com.myqyl.aitradex.etrade.client;

import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.oauth.EtradeTokenService;
import com.myqyl.aitradex.etrade.repository.EtradeAuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.slf4j.MDC;
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
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public EtradeApiClient(
      EtradeOAuth1Template oauthTemplate,
      EtradeTokenService tokenService,
      EtradeAuditLogRepository auditLogRepository,
      ObjectMapper objectMapper) {
    this.oauthTemplate = oauthTemplate;
    this.tokenService = tokenService;
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
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

  /**
   * Makes a non-OAuth request (e.g., for delayed quotes with consumerKey).
   * Used when OAuth is not initialized or not required.
   */
  public String makeRequestWithoutOAuth(String method, String url, Map<String, String> queryParams,
                                       String requestBody) {
    return makeRequestWithoutOAuthWithRetry(method, url, queryParams, requestBody, 0);
  }

  private String makeRequestWithoutOAuthWithRetry(String method, String url, Map<String, String> queryParams,
                                                  String requestBody, int retryCount) {
    Instant startTime = Instant.now();
    String action = method + " " + url;
    String auditRequestBody = requestBody;
    
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", "unauthenticated");
    
    try {
      // Build full URL with query params
      String fullUrl = url;
      if (queryParams != null && !queryParams.isEmpty()) {
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        queryParams.forEach((k, v) -> urlBuilder.append(k).append("=").append(v != null ? v : "").append("&"));
        fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);
      }

      // Build HTTP request WITHOUT OAuth header
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(fullUrl))
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
        int waitSeconds = (int) Math.pow(2, retryCount) * 5;
        log.warn("Rate limited (non-OAuth), retrying in {} seconds (attempt {}/{})", 
            waitSeconds, retryCount + 1, MAX_RETRIES);
        Thread.sleep(waitSeconds * 1000L);
        return makeRequestWithoutOAuthWithRetry(method, url, queryParams, requestBody, retryCount + 1);
      }

      // Log to audit (no accountId for non-OAuth requests)
      logAudit(null, action, queryParams != null ? queryParams.toString() : null, 
               auditRequestBody, response.body(), response.statusCode(), null, durationMs);

      // Handle errors
      if (response.statusCode() >= 400) {
        String errorCode = extractErrorCode(response.body());
        String errorMessage = extractErrorMessage(response.body());
        log.error("E*TRADE API error {} [{}]: {}", response.statusCode(), errorCode, errorMessage);
        throw new EtradeApiException(response.statusCode(), errorCode, errorMessage);
      }

      return response.body();
    } catch (EtradeApiException e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, auditRequestBody, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, auditRequestBody, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(null, action, null, auditRequestBody, null, null, e.getMessage(), durationMs);
      log.error("E*TRADE API request failed (non-OAuth) [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "E*TRADE API request failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  private String makeRequestWithRetry(String method, String url, Map<String, String> queryParams,
                                     String requestBody, UUID accountId, int retryCount) {
    Instant startTime = Instant.now();
    String action = method + " " + url;
    String auditRequestBody = requestBody;
    
    // Generate correlation ID for request tracking
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);
    MDC.put("accountId", accountId != null ? accountId.toString() : "unknown");
    
    try {
      // Get access token for account
      Optional<EtradeTokenService.AccessTokenPair> tokenOpt = tokenService.getAccessToken(accountId);
      if (tokenOpt.isEmpty()) {
        throw new EtradeApiException(401, "NO_TOKEN", 
            "No access token found for account " + accountId);
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
        String errorCode = extractErrorCode(response.body());
        String errorMessage = extractErrorMessage(response.body());
        log.error("E*TRADE API error {} [{}]: {}", response.statusCode(), errorCode, errorMessage);
        throw new EtradeApiException(response.statusCode(), errorCode, errorMessage);
      }

      return response.body();
    } catch (EtradeApiException e) {
      // Re-throw EtradeApiException as-is
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(accountId, action, null, auditRequestBody, null, e.getHttpStatus(), 
          e.getErrorCode() + ": " + e.getErrorMessage(), durationMs);
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(accountId, action, null, auditRequestBody, null, null, "Request interrupted", durationMs);
      throw new EtradeApiException(500, "INTERRUPTED", "Request interrupted", e);
    } catch (Exception e) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      logAudit(accountId, action, null, auditRequestBody, null, null, e.getMessage(), durationMs);
      log.error("E*TRADE API request failed [correlationId={}]", correlationId, e);
      throw new EtradeApiException(500, "INTERNAL_ERROR", 
          "E*TRADE API request failed: " + e.getMessage(), e);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("accountId");
    }
  }

  /**
   * Extracts error code from E*TRADE API error response.
   */
  private String extractErrorCode(String responseBody) {
    if (responseBody == null || responseBody.isEmpty()) {
      return "UNKNOWN";
    }
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode messagesNode = root.path("Messages");
      if (!messagesNode.isMissingNode()) {
        if (messagesNode.isArray() && messagesNode.size() > 0) {
          return messagesNode.get(0).path("code").asText("UNKNOWN");
        } else if (messagesNode.isObject()) {
          return messagesNode.path("code").asText("UNKNOWN");
        }
      }
      // Try error field
      JsonNode errorNode = root.path("error");
      if (!errorNode.isMissingNode()) {
        return errorNode.asText("UNKNOWN");
      }
    } catch (Exception e) {
      log.debug("Failed to parse error code from response", e);
    }
    return "UNKNOWN";
  }

  /**
   * Extracts error message from E*TRADE API error response.
   */
  private String extractErrorMessage(String responseBody) {
    if (responseBody == null || responseBody.isEmpty()) {
      return "Unknown error";
    }
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode messagesNode = root.path("Messages");
      if (!messagesNode.isMissingNode()) {
        StringBuilder errorMsg = new StringBuilder();
        if (messagesNode.isArray()) {
          for (JsonNode msgNode : messagesNode) {
            String desc = msgNode.path("description").asText("");
            if (!desc.isEmpty()) {
              if (errorMsg.length() > 0) {
                errorMsg.append("; ");
              }
              errorMsg.append(desc);
            }
          }
        } else if (messagesNode.isObject()) {
          errorMsg.append(messagesNode.path("description").asText("Unknown error"));
        }
        if (errorMsg.length() > 0) {
          return errorMsg.toString();
        }
      }
      // Try error field
      JsonNode errorNode = root.path("error");
      if (!errorNode.isMissingNode()) {
        return errorNode.asText("Unknown error");
      }
      // Fallback: return first 200 chars of response
      return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    } catch (Exception e) {
      log.debug("Failed to parse error message from response", e);
      return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }
  }

  private void logAudit(UUID accountId, String action, String queryParams, String requestBody,
                       String responseBody, Integer statusCode, String errorMessage, long durationMs) {
    try {
      com.myqyl.aitradex.etrade.domain.EtradeAuditLog auditLog = 
          new com.myqyl.aitradex.etrade.domain.EtradeAuditLog();
      auditLog.setAccountId(accountId);
      auditLog.setAction(action);
      // Note: requestParams field not in entity, storing in requestBody if query params exist
      auditLog.setRequestBody(requestBody != null ? 
          (requestBody.length() > 1000 ? requestBody.substring(0, 1000) + "..." : requestBody) : null);
      auditLog.setResponseBody(responseBody != null ? 
          (responseBody.length() > 5000 ? responseBody.substring(0, 5000) + "..." : responseBody) : null);
      auditLog.setStatusCode(statusCode);
      auditLog.setErrorMessage(errorMessage);
      auditLog.setDurationMs((int) durationMs);
      auditLog.setCreatedAt(java.time.OffsetDateTime.now());
      auditLogRepository.save(auditLog);
      
      // Log with correlation ID
      String correlationId = MDC.get("correlationId");
      if (correlationId != null) {
        log.debug("Audit logged [correlationId={}, action={}, status={}, duration={}ms]", 
            correlationId, action, statusCode, durationMs);
      }
    } catch (Exception e) {
      log.warn("Failed to log audit entry", e);
    }
  }
}
