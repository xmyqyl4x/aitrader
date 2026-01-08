package com.myqyl.aitradex.etrade.oauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
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
 * Unit tests for EtradeOAuthService.
 */
@ExtendWith(MockitoExtension.class)
class EtradeOAuthServiceTest {

  @Mock
  private EtradeProperties properties;

  @Mock
  private EtradeOAuth1Template oauthTemplate;

  @Mock
  private EtradeTokenEncryption tokenEncryption;

  @Mock
  private EtradeOAuthTokenRepository tokenRepository;

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private EtradeOAuthService oauthService;
  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    oauthService = new EtradeOAuthService(properties, oauthTemplate, tokenEncryption, tokenRepository);

    // Inject mock HttpClient using reflection
    Field httpClientField = EtradeOAuthService.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(oauthService, httpClient);

    // Setup properties
    when(properties.getOAuthRequestTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/request_token");
    when(properties.getOAuthAccessTokenUrl())
        .thenReturn("https://apisb.etrade.com/oauth/access_token");
    when(properties.getAuthorizeUrl())
        .thenReturn("https://us.etrade.com/e/t/etws/authorize");
    when(properties.getConsumerKey())
        .thenReturn("test_consumer_key");
    when(properties.getCallbackUrl())
        .thenReturn("http://localhost:4200/etrade-review-trade/callback");

    // Setup OAuth template
    when(oauthTemplate.generateAuthorizationHeader(
        eq("GET"), anyString(), any(), isNull(), isNull()))
        .thenReturn("OAuth oauth_consumer_key=\"test\", oauth_signature=\"sig1\", ...");

    when(oauthTemplate.parseOAuthResponse(anyString()))
        .thenAnswer(invocation -> {
          String response = invocation.getArgument(0);
          Map<String, String> params = new java.util.HashMap<>();
          if (response.contains("oauth_token=")) {
            params.put("oauth_token", "request_token_123");
            params.put("oauth_token_secret", "request_secret_123");
          }
          if (response.contains("access_token")) {
            params.put("oauth_token", "access_token_456");
            params.put("oauth_token_secret", "access_secret_456");
          }
          return params;
        });
  }

  @Test
  void getRequestToken_successful_returnsAuthorizationUrl() throws Exception {
    String mockResponse = "oauth_token=request_token_123&oauth_token_secret=request_secret_123";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    EtradeOAuthService.RequestTokenResponse response = oauthService.getRequestToken(TEST_USER_ID);

    assertNotNull(response);
    assertNotNull(response.getAuthorizationUrl());
    assertTrue(response.getAuthorizationUrl().contains("authorize"));
    assertTrue(response.getAuthorizationUrl().contains("request_token_123"));
    assertEquals("request_token_123", response.getRequestToken());
    assertEquals("request_secret_123", response.getRequestTokenSecret());

    // Verify GET method is used
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), any());
    assertEquals("GET", requestCaptor.getValue().method());
  }

  @Test
  void getRequestToken_httpError_throwsException() throws Exception {
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.body()).thenReturn("Internal Server Error");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.getRequestToken(TEST_USER_ID));

    assertTrue(exception.getMessage().contains("Failed to get request token"));
  }

  @Test
  void getRequestToken_invalidResponse_throwsException() throws Exception {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("invalid_response");
    when(oauthTemplate.parseOAuthResponse("invalid_response"))
        .thenReturn(Map.of()); // Empty map

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.getRequestToken(TEST_USER_ID));

    assertTrue(exception.getMessage().contains("Invalid request token response"));
  }

  @Test
  void exchangeForAccessToken_successful_storesToken() throws Exception {
    String mockResponse = "oauth_token=access_token_456&oauth_token_secret=access_secret_456";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Mock encryption
    when(tokenEncryption.encrypt("access_token_456")).thenReturn("encrypted_access_token");
    when(tokenEncryption.encrypt("access_secret_456")).thenReturn("encrypted_access_secret");

    // Mock repository
    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.empty());
    when(tokenRepository.save(any(EtradeOAuthToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, String> result = oauthService.exchangeForAccessToken(
        "request_token_123", "request_secret_123", "verifier_789", TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertEquals("access_token_456", result.get("oauth_token"));
    assertEquals("access_secret_456", result.get("oauth_token_secret"));

    // Verify token is saved
    verify(tokenRepository, times(1)).save(any(EtradeOAuthToken.class));
    verify(tokenEncryption, times(2)).encrypt(anyString());

    // Verify GET method is used
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), any());
    assertEquals("GET", requestCaptor.getValue().method());
  }

  @Test
  void exchangeForAccessToken_httpError_throwsException() throws Exception {
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("Unauthorized");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.exchangeForAccessToken(
            "request_token_123", "request_secret_123", "verifier_789", TEST_ACCOUNT_ID));

    assertTrue(exception.getMessage().contains("Failed to exchange access token"));
  }

  @Test
  void getAccessToken_returnsDecryptedToken() {
    EtradeOAuthToken token = new EtradeOAuthToken();
    token.setAccountId(TEST_ACCOUNT_ID);
    token.setAccessTokenEncrypted("encrypted_access_token");
    token.setAccessTokenSecretEncrypted("encrypted_access_secret");
    token.setCreatedAt(OffsetDateTime.now());

    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.of(token));
    when(tokenEncryption.decrypt("encrypted_access_token"))
        .thenReturn("decrypted_access_token");
    when(tokenEncryption.decrypt("encrypted_access_secret"))
        .thenReturn("decrypted_access_secret");

    EtradeOAuthService.AccessTokenPair result = oauthService.getAccessToken(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertEquals("decrypted_access_token", result.getAccessToken());
    assertEquals("decrypted_access_secret", result.getAccessTokenSecret());
  }

  @Test
  void getAccessToken_noToken_throwsException() {
    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.getAccessToken(TEST_ACCOUNT_ID));

    assertTrue(exception.getMessage().contains("No access token found"));
  }
}
