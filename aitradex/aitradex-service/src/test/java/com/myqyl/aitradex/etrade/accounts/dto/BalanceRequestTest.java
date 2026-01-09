package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BalanceRequest DTO.
 */
@DisplayName("BalanceRequest DTO Tests")
class BalanceRequestTest {

  @Test
  @DisplayName("Should create BalanceRequest with default values")
  void shouldCreateWithDefaults() {
    BalanceRequest request = new BalanceRequest();

    assertEquals("BROKERAGE", request.getInstType());
    assertNull(request.getAccountType());
    assertTrue(request.getRealTimeNAV());
  }

  @Test
  @DisplayName("Should create BalanceRequest with all parameters")
  void shouldCreateWithAllParameters() {
    BalanceRequest request = new BalanceRequest("CASH", "INDIVIDUAL", false);

    assertEquals("CASH", request.getInstType());
    assertEquals("INDIVIDUAL", request.getAccountType());
    assertFalse(request.getRealTimeNAV());
  }

  @Test
  @DisplayName("Should handle null parameters with defaults")
  void shouldHandleNullParameters() {
    BalanceRequest request = new BalanceRequest(null, null, null);

    assertEquals("BROKERAGE", request.getInstType());
    assertNull(request.getAccountType());
    assertTrue(request.getRealTimeNAV());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    BalanceRequest request = new BalanceRequest();

    request.setInstType("MARGIN");
    request.setAccountType("JOINT");
    request.setRealTimeNAV(false);

    assertEquals("MARGIN", request.getInstType());
    assertEquals("JOINT", request.getAccountType());
    assertFalse(request.getRealTimeNAV());
  }
}
