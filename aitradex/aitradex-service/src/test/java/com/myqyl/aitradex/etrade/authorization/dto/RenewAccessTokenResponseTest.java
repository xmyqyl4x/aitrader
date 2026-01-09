package com.myqyl.aitradex.etrade.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RenewAccessTokenResponse DTO.
 */
@DisplayName("RenewAccessTokenResponse DTO Tests")
class RenewAccessTokenResponseTest {

  @Test
  @DisplayName("Should create RenewAccessTokenResponse with message")
  void shouldCreateRenewAccessTokenResponseWithMessage() {
    String message = "Access Token has been renewed";

    RenewAccessTokenResponse response = new RenewAccessTokenResponse(message);

    assertEquals(message, response.getMessage());
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("Should create RenewAccessTokenResponse with default constructor")
  void shouldCreateRenewAccessTokenResponseWithDefaultConstructor() {
    RenewAccessTokenResponse response = new RenewAccessTokenResponse();

    assertNull(response.getMessage());
    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("Should set and get message")
  void shouldSetAndGetMessage() {
    RenewAccessTokenResponse response = new RenewAccessTokenResponse();

    response.setMessage("Token renewed successfully");

    assertEquals("Token renewed successfully", response.getMessage());
    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should return true when message contains 'renewed'")
  void isSuccessShouldReturnTrueWhenMessageContainsRenewed() {
    RenewAccessTokenResponse response = new RenewAccessTokenResponse();
    response.setMessage("Access Token has been renewed");

    assertTrue(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should return false when message does not contain 'renewed'")
  void isSuccessShouldReturnFalseWhenMessageDoesNotContainRenewed() {
    RenewAccessTokenResponse response = new RenewAccessTokenResponse();
    response.setMessage("Error occurred");

    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should handle null message")
  void isSuccessShouldHandleNullMessage() {
    RenewAccessTokenResponse response = new RenewAccessTokenResponse();

    assertFalse(response.isSuccess());
  }

  @Test
  @DisplayName("isSuccess should handle case insensitive")
  void isSuccessShouldHandleCaseInsensitive() {
    RenewAccessTokenResponse response1 = new RenewAccessTokenResponse("Token RENEWED");
    RenewAccessTokenResponse response2 = new RenewAccessTokenResponse("Renewed token");

    assertTrue(response1.isSuccess());
    assertTrue(response2.isSuccess());
  }
}
