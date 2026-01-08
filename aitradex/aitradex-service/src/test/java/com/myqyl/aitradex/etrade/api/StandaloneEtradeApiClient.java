package com.myqyl.aitradex.etrade.api;

import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone E*TRADE API client for testing.
 * Does not require Spring Boot context or database.
 * Uses OAuth 1.0a signing with provided access token and secret.
 */
public class StandaloneEtradeApiClient {

  private static final Logger log = LoggerFactory.getLogger(StandaloneEtradeApiClient.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final String baseUrl;
  private final EtradeOAuth1Template oauthTemplate;
  private final HttpClient httpClient;
  private final String accessToken;
  private final String accessTokenSecret;

  /**
   * Creates a standalone API client.
   *
   * @param baseUrl E*TRADE API base URL (e.g., https://apisb.etrade.com)
   * @param consumerKey E*TRADE consumer key
   * @param consumerSecret E*TRADE consumer secret
   * @param accessToken OAuth access token
   * @param accessTokenSecret OAuth access token secret
   */
  public StandaloneEtradeApiClient(String baseUrl, String consumerKey, String consumerSecret,
                                   String accessToken, String accessTokenSecret) {
    this.baseUrl = baseUrl;
    this.oauthTemplate = new EtradeOAuth1Template(consumerKey, consumerSecret);
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
    
    // Store access token for use in requests
    this.accessToken = accessToken;
    this.accessTokenSecret = accessTokenSecret;
  }

  /**
   * Makes an authenticated GET request to E*TRADE API.
   *
   * @param endpoint API endpoint (e.g., "/v1/accounts/list")
   * @param queryParams Optional query parameters
   * @return Response body as String (XML)
   */
  public String get(String endpoint, Map<String, String> queryParams) {
    try {
      // Build full URL
      String url = baseUrl + endpoint;
      if (queryParams != null && !queryParams.isEmpty()) {
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        queryParams.forEach((k, v) -> {
          if (v != null && !v.isEmpty()) {
            urlBuilder.append(k).append("=").append(v).append("&");
          }
        });
        url = urlBuilder.substring(0, urlBuilder.length() - 1);
      }

      // Generate OAuth authorization header
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          "GET", url, queryParams != null ? queryParams : new HashMap<>(), 
          accessToken, accessTokenSecret);

      // Build HTTP request
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Accept", "application/xml") // E*TRADE returns XML
          .timeout(REQUEST_TIMEOUT)
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("API request failed with status {}: {}", response.statusCode(), response.body());
        throw new RuntimeException("API request failed: " + response.statusCode() + " - " + response.body());
      }

      return response.body();
    } catch (Exception e) {
      log.error("Failed to make API request to {}", endpoint, e);
      throw new RuntimeException("API request failed", e);
    }
  }

  /**
   * Makes an authenticated GET request without query parameters.
   */
  public String get(String endpoint) {
    return get(endpoint, null);
  }

  /**
   * Makes an authenticated POST request to E*TRADE API with JSON body.
   *
   * @param endpoint API endpoint (e.g., "/v1/accounts/{accountIdKey}/orders/preview")
   * @param requestBody JSON request body
   * @return Response body as String (JSON for Order API, XML for others)
   */
  public String post(String endpoint, String requestBody) {
    return post(endpoint, null, requestBody);
  }

  /**
   * Makes an authenticated POST request to E*TRADE API with query parameters and JSON body.
   *
   * @param endpoint API endpoint
   * @param queryParams Optional query parameters
   * @param requestBody JSON request body
   * @return Response body as String (JSON for Order API, XML for others)
   */
  public String post(String endpoint, Map<String, String> queryParams, String requestBody) {
    return makeRequestWithBody("POST", endpoint, queryParams, requestBody);
  }

  /**
   * Makes an authenticated PUT request to E*TRADE API with JSON body.
   *
   * @param endpoint API endpoint (e.g., "/v1/accounts/{accountIdKey}/orders/cancel")
   * @param requestBody JSON request body
   * @return Response body as String (JSON for Order API, XML for others)
   */
  public String put(String endpoint, String requestBody) {
    return put(endpoint, null, requestBody);
  }

  /**
   * Makes an authenticated PUT request to E*TRADE API with query parameters and JSON body.
   *
   * @param endpoint API endpoint
   * @param queryParams Optional query parameters
   * @param requestBody JSON request body
   * @return Response body as String (JSON for Order API, XML for others)
   */
  public String put(String endpoint, Map<String, String> queryParams, String requestBody) {
    return makeRequestWithBody("PUT", endpoint, queryParams, requestBody);
  }

  /**
   * Internal method to make POST/PUT requests with body.
   */
  private String makeRequestWithBody(String method, String endpoint, Map<String, String> queryParams, String requestBody) {
    try {
      // Build full URL
      String url = baseUrl + endpoint;
      if (queryParams != null && !queryParams.isEmpty()) {
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        queryParams.forEach((k, v) -> {
          if (v != null && !v.isEmpty()) {
            urlBuilder.append(k).append("=").append(v).append("&");
          }
        });
        url = urlBuilder.substring(0, urlBuilder.length() - 1);
      }

      // Generate OAuth authorization header
      String authHeader = oauthTemplate.generateAuthorizationHeader(
          method, url, queryParams != null ? queryParams : new HashMap<>(), 
          accessToken, accessTokenSecret);

      // Build HTTP request
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/json")
          .header("Accept", "application/json") // Order API returns JSON
          .timeout(REQUEST_TIMEOUT);

      if (requestBody != null && !requestBody.isEmpty()) {
        requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(requestBody));
      } else {
        requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
      }

      HttpRequest request = requestBuilder.build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("API request failed with status {}: {}", response.statusCode(), response.body());
        throw new RuntimeException("API request failed: " + response.statusCode() + " - " + response.body());
      }

      return response.body();
    } catch (Exception e) {
      log.error("Failed to make API request to {}", endpoint, e);
      throw new RuntimeException("API request failed", e);
    }
  }
}
