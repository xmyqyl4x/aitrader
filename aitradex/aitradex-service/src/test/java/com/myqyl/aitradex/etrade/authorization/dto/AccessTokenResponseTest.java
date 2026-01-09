package com.myqyl.aitradex.etrade.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccessTokenResponse DTO.
 */
@DisplayName("AccessTokenResponse DTO Tests")
class AccessTokenResponseTest {

  @Test
  @DisplayName("Should create AccessTokenResponse with all fields")
  void shouldCreateAccessTokenResponseWithAllFields() {
    String token = "access_token_123";
    String secret = "access_secret_456";

    AccessTokenResponse response = new AccessTokenResponse(token, secret);

    assertEquals(token, response.getOauthToken());
    assertEquals(secret, response.getOauthTokenSecret());
  }

  @Test
  @DisplayName("Should create AccessTokenResponse with default constructor")
  void shouldCreateAccessTokenResponseWithDefaultConstructor() {
    AccessTokenResponse response = new AccessTokenResponse();

    assertNull(response.getOauthToken());
    assertNull(response.getOauthTokenSecret());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    AccessTokenResponse response = new AccessTokenResponse();

    response.setOauthToken("new_token");
    response.setOauthTokenSecret("new_secret");

    assertEquals("new_token", response.getOauthToken());
    assertEquals("new_secret", response.getOauthTokenSecret());
  }
}
