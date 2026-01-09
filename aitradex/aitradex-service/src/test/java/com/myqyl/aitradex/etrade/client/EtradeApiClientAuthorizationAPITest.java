package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.oauth.EtradeOAuth1Template;
import com.myqyl.aitradex.etrade.repository.EtradeAuditLogRepository;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeApiClientAuthorizationAPI.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeApiClientAuthorizationAPI Tests")
class EtradeApiClientAuthorizationAPITest {

  @Mock
  private EtradeProperties properties;

  @Mock
  private EtradeOAuth1Template oauthTemplate;

  @Mock
  private EtradeAuditLogRepository auditLogRepository;

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private EtradeApiClientAuthorizationAPI authorizationApi;

  @BeforeEach
  void setUp() throws Exception {
    authorizationApi = new EtradeApiClientAuthorizationAPI(
        properties, oauthTemplate, auditLogRepository);

    // Inject mock HttpClient using reflection
    Field httpClientField = EtradeApiClientAuthorizationAPI.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(authorizationApi, httpClient);

    // Setup properties
    when(properties.getOAuthRequestTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/request_token");
    when(properties.getOAuthAccessTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/access_token");
    when(properties.getOAuthRenewAccessTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/renew_access_token");
    when(properties.getOAuthRevokeAccessTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/revoke_access_token");
    when(properties.getAuthorizeUrl())
        .thenReturn("https://us.etrade.com/e/t/etws/authorize");
    when(properties.getConsumerKey())
        .thenReturn("test_consumer_key");

    // Setup OAuth template
    when(oauthTemplate.generateAuthorizationHeader(
        eq("GET"), anyString(), any(), isNull(), isNull()))
        .thenReturn("OAuth oauth_consumer_key=\"test\", oauth_signature=\"sig1\", ...");

    when(oauthTemplate.generateAuthorizationHeader(
        eq("GET"), anyString(), any(), anyString(), anyString()))
        .thenReturn("OAuth oauth_consumer_key=\"test\", oauth_token=\"token\", oauth_signature=\"sig2\", ...");

    when(oauthTemplate.parseOAuthResponse(anyString()))
        .thenAnswer(invocation -> {
          String response = invocation.getArgument(0);
          Map<String, String> params = new HashMap<>();
          if (response.contains("oauth_token=")) {
            String[] pairs = response.split("&");
            for (String pair : pairs) {
              String[] kv = pair.split("=", 2);
              if (kv.length == 2) {
                params.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
              }
            }
          }
          return params;
        });
  }

  @Test
  @DisplayName("getRequestToken should return RequestTokenResponse on success")
  void getRequestTokenShouldReturnRequestTokenResponseOnSuccess() throws Exception {
    String mockResponse = "oauth_token=request_token_123&oauth_token_secret=request_secret_456&oauth_callback_confirmed=true";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RequestTokenRequest request = new RequestTokenRequest("oob");
    RequestTokenResponse response = authorizationApi.getRequestToken(request);

    assertNotNull(response);
    assertEquals("request_token_123", response.getOauthToken());
    assertEquals("request_secret_456", response.getOauthTokenSecret());
    assertEquals("true", response.getOauthCallbackConfirmed());
    assertTrue(response.isCallbackConfirmed());
    verify(auditLogRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("getRequestToken should throw EtradeApiException on HTTP error")
  void getRequestTokenShouldThrowEtradeApiExceptionOnHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.body()).thenReturn("Bad Request");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RequestTokenRequest request = new RequestTokenRequest("oob");

    EtradeApiException exception = assertThrows(
        EtradeApiException.class,
        () -> authorizationApi.getRequestToken(request));

    assertEquals(400, exception.getHttpStatus());
    assertEquals("REQUEST_TOKEN_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("getRequestToken should throw EtradeApiException when response is missing required fields")
  void getRequestTokenShouldThrowExceptionWhenMissingFields() throws Exception {
    String mockResponse = "oauth_token=token123"; // Missing oauth_token_secret

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RequestTokenRequest request = new RequestTokenRequest("oob");

    EtradeApiException exception = assertThrows(
        EtradeApiException.class,
        () -> authorizationApi.getRequestToken(request));

    assertEquals(400, exception.getHttpStatus());
    assertEquals("INVALID_RESPONSE", exception.getErrorCode());
  }

  @Test
  @DisplayName("authorizeApplication should return authorization URL")
  void authorizeApplicationShouldReturnAuthorizationUrl() {
    AuthorizeApplicationRequest request = new AuthorizeApplicationRequest(
        "test_consumer_key", "request_token_123");

    AuthorizeApplicationResponse response = authorizationApi.authorizeApplication(request);

    assertNotNull(response);
    assertTrue(response.getAuthorizationUrl().contains("test_consumer_key"));
    assertTrue(response.getAuthorizationUrl().contains("request_token_123"));
    assertTrue(response.getAuthorizationUrl().startsWith("https://us.etrade.com/e/t/etws/authorize"));
  }

  @Test
  @DisplayName("getAccessToken should return AccessTokenResponse on success")
  void getAccessTokenShouldReturnAccessTokenResponseOnSuccess() throws Exception {
    String mockResponse = "oauth_token=access_token_789&oauth_token_secret=access_secret_012";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    AccessTokenRequest request = new AccessTokenRequest(
        "request_token_123", "request_secret_456", "verifier_789");

    AccessTokenResponse response = authorizationApi.getAccessToken(request);

    assertNotNull(response);
    assertEquals("access_token_789", response.getOauthToken());
    assertEquals("access_secret_012", response.getOauthTokenSecret());
    verify(auditLogRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("getAccessToken should throw EtradeApiException on HTTP error")
  void getAccessTokenShouldThrowEtradeApiExceptionOnHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("Unauthorized");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    AccessTokenRequest request = new AccessTokenRequest(
        "request_token_123", "request_secret_456", "invalid_verifier");

    EtradeApiException exception = assertThrows(
        EtradeApiException.class,
        () -> authorizationApi.getAccessToken(request));

    assertEquals(401, exception.getHttpStatus());
    assertEquals("ACCESS_TOKEN_EXCHANGE_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("renewAccessToken should return RenewAccessTokenResponse on success")
  void renewAccessTokenShouldReturnRenewAccessTokenResponseOnSuccess() throws Exception {
    String mockResponse = "Access Token has been renewed";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RenewAccessTokenRequest request = new RenewAccessTokenRequest(
        "access_token_123", "access_secret_456");

    RenewAccessTokenResponse response = authorizationApi.renewAccessToken(request);

    assertNotNull(response);
    assertTrue(response.getMessage().toLowerCase().contains("renewed"));
    assertTrue(response.isSuccess());
    verify(auditLogRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("renewAccessToken should throw EtradeApiException on HTTP error")
  void renewAccessTokenShouldThrowEtradeApiExceptionOnHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("Unauthorized");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RenewAccessTokenRequest request = new RenewAccessTokenRequest(
        "invalid_token", "invalid_secret");

    EtradeApiException exception = assertThrows(
        EtradeApiException.class,
        () -> authorizationApi.renewAccessToken(request));

    assertEquals(401, exception.getHttpStatus());
    assertEquals("RENEW_TOKEN_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("revokeAccessToken should return RevokeAccessTokenResponse on success")
  void revokeAccessTokenShouldReturnRevokeAccessTokenResponseOnSuccess() throws Exception {
    String mockResponse = "Revoked Access Token";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RevokeAccessTokenRequest request = new RevokeAccessTokenRequest(
        "access_token_123", "access_secret_456");

    RevokeAccessTokenResponse response = authorizationApi.revokeAccessToken(request);

    assertNotNull(response);
    assertTrue(response.getMessage().toLowerCase().contains("revoked"));
    assertTrue(response.isSuccess());
    verify(auditLogRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("revokeAccessToken should throw EtradeApiException on HTTP error")
  void revokeAccessTokenShouldThrowEtradeApiExceptionOnHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("Unauthorized");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RevokeAccessTokenRequest request = new RevokeAccessTokenRequest(
        "invalid_token", "invalid_secret");

    EtradeApiException exception = assertThrows(
        EtradeApiException.class,
        () -> authorizationApi.revokeAccessToken(request));

    assertEquals(401, exception.getHttpStatus());
    assertEquals("REVOKE_TOKEN_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("getRequestToken should include oauth_callback in authorization header")
  void getRequestTokenShouldIncludeOAuthCallbackInHeader() throws Exception {
    String mockResponse = "oauth_token=token&oauth_token_secret=secret&oauth_callback_confirmed=true";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RequestTokenRequest request = new RequestTokenRequest("oob");
    authorizationApi.getRequestToken(request);

    ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(oauthTemplate).generateAuthorizationHeader(
        eq("GET"), anyString(), paramsCaptor.capture(), isNull(), isNull());

    Map<String, String> params = paramsCaptor.getValue();
    assertEquals("oob", params.get("oauth_callback"));
  }
}
