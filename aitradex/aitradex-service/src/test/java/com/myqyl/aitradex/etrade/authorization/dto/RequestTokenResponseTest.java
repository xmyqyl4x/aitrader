package com.myqyl.aitradex.etrade.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequestTokenResponse DTO.
 */
@DisplayName("RequestTokenResponse DTO Tests")
class RequestTokenResponseTest {

  @Test
  @DisplayName("Should create RequestTokenResponse with all fields")
  void shouldCreateRequestTokenResponseWithAllFields() {
    String token = "test_oauth_token";
    String secret = "test_oauth_token_secret";
    String confirmed = "true";

    RequestTokenResponse response = new RequestTokenResponse(token, secret, confirmed);

    assertEquals(token, response.getOauthToken());
    assertEquals(secret, response.getOauthTokenSecret());
    assertEquals(confirmed, response.getOauthCallbackConfirmed());
    assertTrue(response.isCallbackConfirmed());
  }

  @Test
  @DisplayName("Should create RequestTokenResponse with default constructor")
  void shouldCreateRequestTokenResponseWithDefaultConstructor() {
    RequestTokenResponse response = new RequestTokenResponse();

    assertNull(response.getOauthToken());
    assertNull(response.getOauthTokenSecret());
    assertNull(response.getOauthCallbackConfirmed());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    RequestTokenResponse response = new RequestTokenResponse();

    response.setOauthToken("token123");
    response.setOauthTokenSecret("secret456");
    response.setOauthCallbackConfirmed("true");

    assertEquals("token123", response.getOauthToken());
    assertEquals("secret456", response.getOauthTokenSecret());
    assertEquals("true", response.getOauthCallbackConfirmed());
    assertTrue(response.isCallbackConfirmed());
  }

  @Test
  @DisplayName("isCallbackConfirmed should return false when callback not confirmed")
  void isCallbackConfirmedShouldReturnFalseWhenNotConfirmed() {
    RequestTokenResponse response = new RequestTokenResponse(
        "token", "secret", "false");

    assertFalse(response.isCallbackConfirmed());
  }

  @Test
  @DisplayName("isCallbackConfirmed should handle case insensitive")
  void isCallbackConfirmedShouldHandleCaseInsensitive() {
    RequestTokenResponse response1 = new RequestTokenResponse("token", "secret", "TRUE");
    RequestTokenResponse response2 = new RequestTokenResponse("token", "secret", "True");

    assertTrue(response1.isCallbackConfirmed());
    assertTrue(response2.isCallbackConfirmed());
  }
}
