package com.myqyl.aitradex.etrade.oauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myqyl.aitradex.etrade.authorization.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAuthorizationAPI;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.domain.EtradeOAuthToken;
import com.myqyl.aitradex.etrade.repository.EtradeOAuthTokenRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeOAuthService.
 * 
 * @deprecated This mocked unit test has been replaced by functional tests that make real calls to E*TRADE.
 * Use {@link EtradeOAuthFunctionalTest} instead, which validates the complete OAuth flow through our
 * REST API endpoints and validates database persistence.
 * 
 * This test is kept for reference but should not be used for validating OAuth functionality.
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeOAuthService Tests (DEPRECATED - Use Functional Tests Instead)")
class EtradeOAuthServiceTest {

  @Mock
  private EtradeProperties properties;

  @Mock
  private EtradeApiClientAuthorizationAPI authorizationApi;

  @Mock
  private EtradeTokenEncryption tokenEncryption;

  @Mock
  private EtradeOAuthTokenRepository tokenRepository;

  private EtradeOAuthService oauthService;
  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    oauthService = new EtradeOAuthService(properties, authorizationApi, tokenEncryption, tokenRepository);

    // Setup properties
    when(properties.getEnvironment())
        .thenReturn(EtradeProperties.Environment.SANDBOX);
    when(properties.getCallbackUrl())
        .thenReturn("http://localhost:4200/etrade-review-trade/callback");
    when(properties.getConsumerKey())
        .thenReturn("test_consumer_key");
    when(properties.getAuthorizeUrl())
        .thenReturn("https://us.etrade.com/e/t/etws/authorize");
  }

  @Test
  @DisplayName("getRequestToken should return RequestTokenResponse on success")
  void getRequestTokenShouldReturnRequestTokenResponseOnSuccess() {
    // Mock Authorization API response
    RequestTokenResponse apiResponse = new RequestTokenResponse(
        "request_token_123", "request_secret_456", "true");
    when(authorizationApi.getRequestToken(any(RequestTokenRequest.class)))
        .thenReturn(apiResponse);

    // Mock authorization URL generation
    AuthorizeApplicationResponse authorizeResponse = new AuthorizeApplicationResponse(
        "https://us.etrade.com/e/t/etws/authorize?key=test_consumer_key&token=request_token_123");
    when(authorizationApi.authorizeApplication(any(AuthorizeApplicationRequest.class)))
        .thenReturn(authorizeResponse);

    EtradeOAuthService.RequestTokenResponse response = oauthService.getRequestToken(TEST_USER_ID);

    assertNotNull(response);
    assertNotNull(response.getAuthorizationUrl());
    assertTrue(response.getAuthorizationUrl().contains("authorize"));
    assertTrue(response.getAuthorizationUrl().contains("request_token_123"));
    assertEquals("request_token_123", response.getRequestToken());
    assertEquals("request_secret_456", response.getRequestTokenSecret());

    verify(authorizationApi, times(1)).getRequestToken(any(RequestTokenRequest.class));
    verify(authorizationApi, times(1)).authorizeApplication(any(AuthorizeApplicationRequest.class));
  }

  @Test
  @DisplayName("getRequestToken should handle API exceptions")
  void getRequestTokenShouldHandleApiExceptions() {
    com.myqyl.aitradex.etrade.exception.EtradeApiException apiException =
        new com.myqyl.aitradex.etrade.exception.EtradeApiException(500, "API_ERROR", "API Error");
    when(authorizationApi.getRequestToken(any(RequestTokenRequest.class)))
        .thenThrow(apiException);

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.getRequestToken(TEST_USER_ID));

    assertTrue(exception.getMessage().contains("OAuth request token failed"));
    assertTrue(exception.getCause() == apiException);
  }

  @Test
  @DisplayName("exchangeForAccessToken should store token on success")
  void exchangeForAccessTokenShouldStoreTokenOnSuccess() {
    // Mock Authorization API response
    AccessTokenResponse apiResponse = new AccessTokenResponse(
        "access_token_456", "access_secret_456");
    when(authorizationApi.getAccessToken(any(AccessTokenRequest.class)))
        .thenReturn(apiResponse);

    // Mock encryption
    when(tokenEncryption.encrypt("access_token_456")).thenReturn("encrypted_access_token");
    when(tokenEncryption.encrypt("access_secret_456")).thenReturn("encrypted_access_secret");

    // Mock repository
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
    verify(authorizationApi, times(1)).getAccessToken(any(AccessTokenRequest.class));
  }

  @Test
  @DisplayName("exchangeForAccessToken should handle API exceptions")
  void exchangeForAccessTokenShouldHandleApiExceptions() {
    com.myqyl.aitradex.etrade.exception.EtradeApiException apiException =
        new com.myqyl.aitradex.etrade.exception.EtradeApiException(401, "AUTH_ERROR", "Unauthorized");
    when(authorizationApi.getAccessToken(any(AccessTokenRequest.class)))
        .thenThrow(apiException);

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.exchangeForAccessToken(
            "request_token_123", "request_secret_123", "verifier_789", TEST_ACCOUNT_ID));

    assertTrue(exception.getMessage().contains("Access token exchange failed"));
    assertTrue(exception.getCause() == apiException);
  }

  @Test
  @DisplayName("getAccessToken should return decrypted token")
  void getAccessTokenShouldReturnDecryptedToken() {
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
  @DisplayName("getAccessToken should throw exception when token not found")
  void getAccessTokenShouldThrowExceptionWhenTokenNotFound() {
    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        oauthService.getAccessToken(TEST_ACCOUNT_ID));

    assertTrue(exception.getMessage().contains("No access token found"));
  }

  @Test
  @DisplayName("renewAccessToken should call Authorization API and return response")
  void renewAccessTokenShouldCallAuthorizationApiAndReturnResponse() {
    // Mock token retrieval
    EtradeOAuthToken token = new EtradeOAuthToken();
    token.setAccountId(TEST_ACCOUNT_ID);
    token.setAccessTokenEncrypted("encrypted_token");
    token.setAccessTokenSecretEncrypted("encrypted_secret");
    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.of(token));
    when(tokenEncryption.decrypt("encrypted_token")).thenReturn("access_token");
    when(tokenEncryption.decrypt("encrypted_secret")).thenReturn("access_secret");

    // Mock Authorization API response
    RenewAccessTokenResponse apiResponse = new RenewAccessTokenResponse("Access Token has been renewed");
    when(authorizationApi.renewAccessToken(any(RenewAccessTokenRequest.class)))
        .thenReturn(apiResponse);

    RenewAccessTokenResponse result = oauthService.renewAccessToken(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    verify(authorizationApi, times(1)).renewAccessToken(any(RenewAccessTokenRequest.class));
  }

  @Test
  @DisplayName("revokeAccessToken should call Authorization API and delete token")
  void revokeAccessTokenShouldCallAuthorizationApiAndDeleteToken() {
    // Mock token retrieval
    EtradeOAuthToken token = new EtradeOAuthToken();
    token.setAccountId(TEST_ACCOUNT_ID);
    token.setAccessTokenEncrypted("encrypted_token");
    token.setAccessTokenSecretEncrypted("encrypted_secret");
    when(tokenRepository.findByAccountId(TEST_ACCOUNT_ID))
        .thenReturn(Optional.of(token));
    when(tokenEncryption.decrypt("encrypted_token")).thenReturn("access_token");
    when(tokenEncryption.decrypt("encrypted_secret")).thenReturn("access_secret");

    // Mock Authorization API response
    RevokeAccessTokenResponse apiResponse = new RevokeAccessTokenResponse("Revoked Access Token");
    when(authorizationApi.revokeAccessToken(any(RevokeAccessTokenRequest.class)))
        .thenReturn(apiResponse);

    RevokeAccessTokenResponse result = oauthService.revokeAccessToken(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    verify(authorizationApi, times(1)).revokeAccessToken(any(RevokeAccessTokenRequest.class));
    verify(tokenRepository, times(1)).deleteByAccountId(TEST_ACCOUNT_ID);
  }
}
