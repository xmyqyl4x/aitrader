package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.oauth.EtradeTokenService;
import com.myqyl.aitradex.etrade.repository.EtradeAuditLogRepository;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for EtradeApiClient with mocked HttpClient.
 */
@ExtendWith(MockitoExtension.class)
class EtradeApiClientTest {

  @Mock
  private EtradeOAuth1Template oauthTemplate;

  @Mock
  private EtradeTokenService tokenService;

  @Mock
  private EtradeAuditLogRepository auditLogRepository;

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private EtradeApiClient apiClient;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
  private static final String TEST_ACCESS_TOKEN = "test_access_token";
  private static final String TEST_ACCESS_TOKEN_SECRET = "test_access_token_secret";

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    apiClient = new EtradeApiClient(oauthTemplate, tokenService, auditLogRepository, objectMapper);

    // Inject mock HttpClient using reflection
    Field httpClientField = EtradeApiClient.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(apiClient, httpClient);

    // Mock token service
    when(tokenService.getAccessToken(TEST_ACCOUNT_ID))
        .thenReturn(Optional.of(new EtradeTokenService.AccessTokenPair(
            TEST_ACCESS_TOKEN, TEST_ACCESS_TOKEN_SECRET)));

    // Mock OAuth template
    when(oauthTemplate.generateAuthorizationHeader(
        anyString(), anyString(), any(), eq(TEST_ACCESS_TOKEN), eq(TEST_ACCESS_TOKEN_SECRET)))
        .thenReturn("OAuth oauth_consumer_key=\"test\", oauth_token=\"" + TEST_ACCESS_TOKEN + "\", ...");
  }

  @Test
  void makeRequest_successfulRequest_returnsResponse() throws Exception {
    String mockResponse = """
        {
          "AccountListResponse": {
            "Accounts": {
              "Account": []
            }
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
        null, null, TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertTrue(result.contains("AccountListResponse"));
    verify(auditLogRepository, times(1)).save(any());
  }

  @Test
  void makeRequest_postRequest_withBody_sendsCorrectly() throws Exception {
    String mockResponse = """
        {
          "PreviewOrderResponse": {
            "PreviewIds": [{"previewId": "PREVIEW123"}]
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String requestBody = "{\"PreviewOrderRequest\": {}}";
    String result = apiClient.makeRequest("POST", "https://api.etrade.com/v1/accounts/123/orders/preview",
        null, requestBody, TEST_ACCOUNT_ID);

    assertNotNull(result);
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  @Test
  void makeRequest_withQueryParams_includesInUrl() throws Exception {
    String mockResponse = "{\"response\": \"success\"}";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    Map<String, String> params = Map.of("instType", "BROKERAGE", "realTimeNAV", "true");

    apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/123/balance",
        params, null, TEST_ACCOUNT_ID);

    verify(httpClient).send(requestCaptor.capture(), any());
    HttpRequest capturedRequest = requestCaptor.getValue();
    String uri = capturedRequest.uri().toString();
    assertTrue(uri.contains("instType=BROKERAGE"));
    assertTrue(uri.contains("realTimeNAV=true"));
  }

  @Test
  void makeRequest_httpError_throwsEtradeApiException() throws Exception {
    String errorResponse = """
        {
          "Messages": {
            "Message": [{
              "type": "ERROR",
              "code": "INVALID_REQUEST",
              "description": "Invalid request"
            }]
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.body()).thenReturn(errorResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
            null, null, TEST_ACCOUNT_ID));

    assertEquals(400, exception.getHttpStatus());
    assertNotNull(exception.getErrorCode());
    assertNotNull(exception.getErrorMessage());
  }

  @Test
  void makeRequest_rateLimit_retriesWithBackoff() throws Exception {
    // First attempt: rate limit (429)
    when(httpResponse.statusCode())
        .thenReturn(429)
        .thenReturn(200);
    when(httpResponse.body())
        .thenReturn("Rate limit exceeded")
        .thenReturn("{\"success\": true}");

    @SuppressWarnings("unchecked")
    HttpResponse<String> httpResponse2 = mock(HttpResponse.class);
    when(httpResponse2.statusCode()).thenReturn(200);
    when(httpResponse2.body()).thenReturn("{\"success\": true}");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse)
        .thenReturn(httpResponse2);

    String result = apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
        null, null, TEST_ACCOUNT_ID);

    assertNotNull(result);
    // Should retry after rate limit
    verify(httpClient, atLeast(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void makeRequest_noToken_throwsException() {
    when(tokenService.getAccessToken(TEST_ACCOUNT_ID))
        .thenReturn(Optional.empty());

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
            null, null, TEST_ACCOUNT_ID));

    assertEquals(401, exception.getHttpStatus());
    assertEquals("NO_TOKEN", exception.getErrorCode());
  }

  @Test
  void makeRequest_interrupted_throwsException() throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new InterruptedException("Request interrupted"));

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
            null, null, TEST_ACCOUNT_ID));

    assertEquals(500, exception.getHttpStatus());
    assertEquals("INTERRUPTED", exception.getErrorCode());
  }

  @Test
  void makeRequest_logsAuditEntry() throws Exception {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("{\"success\": true}");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    apiClient.makeRequest("GET", "https://api.etrade.com/v1/accounts/list",
        null, null, TEST_ACCOUNT_ID);

    verify(auditLogRepository, times(1)).save(any());
  }
}
