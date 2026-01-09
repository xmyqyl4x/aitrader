package com.myqyl.aitradex.etrade.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RevokeAccessTokenResponse DTO.
 */
@DisplayName("RevokeAccessTokenResponse DTO Tests")
class RevokeAccessTokenResponseTest {

  @Test
  @DisplayName("Should create RevokeAccessTokenResponse with message only")
  void shouldCreateRevokeAccessTokenResponseWithMessageOnly() {
    String message = "Revoked Access Token";

    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse(message);

    assertEquals(message, response.getMessage());
    assertNull(response.getOauthToken());
    assertNull(response.getOauthTokenSecret());
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("Should create RevokeAccessTokenResponse with all fields")
  void shouldCreateRevokeAccessTokenResponseWithAllFields() {
    String message = "Revoked Access Token";
    String token = "revoked_token";
    String secret = "revoked_secret";

    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse(message, token, secret);

    assertEquals(message, response.getMessage());
    assertEquals(token, response.getOauthToken());
    assertEquals(secret, response.getOauthTokenSecret());
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("Should create RevokeAccessTokenResponse with default constructor")
  void shouldCreateRevokeAccessTokenResponseWithDefaultConstructor() {
    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse();

    assertNull(response.getMessage());
    assertNull(response.getOauthToken());
    assertNull(response.getOauthTokenSecret());
    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse();

    response.setMessage("Token revoked");
    response.setOauthToken("token123");
    response.setOauthTokenSecret("secret456");

    assertEquals("Token revoked", response.getMessage());
    assertEquals("token123", response.getOauthToken());
    assertEquals("secret456", response.getOauthTokenSecret());
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should return true when message contains 'revoked'")
  void isSuccessShouldReturnTrueWhenMessageContainsRevoked() {
    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse();
    response.setMessage("Revoked Access Token");

    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should return false when message does not contain 'revoked'")
  void isSuccessShouldReturnFalseWhenMessageDoesNotContainRevoked() {
    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse();
    response.setMessage("Error occurred");

    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should handle null message")
  void isSuccessShouldHandleNullMessage() {
    RevokeAccessTokenResponse response = new RevokeAccessTokenResponse();

    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should handle case insensitive")
  void isSuccessShouldHandleCaseInsensitive() {
    RevokeAccessTokenResponse response1 = new RevokeAccessTokenResponse("Token REVOKED");
    RevokeAccessTokenResponse response2 = new RevokeAccessTokenResponse("Revoked token");

    assertTrue(response1.isSuccess());
    assertTrue(response2.isSuccess());
  }
}
