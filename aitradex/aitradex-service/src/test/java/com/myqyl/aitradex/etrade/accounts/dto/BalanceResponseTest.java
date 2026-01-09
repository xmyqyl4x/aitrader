package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BalanceResponse DTO.
 */
@DisplayName("BalanceResponse DTO Tests")
class BalanceResponseTest {

  @Test
  @DisplayName("Should create BalanceResponse with all fields")
  void shouldCreateWithAllFields() {
    BalanceResponse response = new BalanceResponse();
    response.setAccountId("account123");
    response.setAccountType("INDIVIDUAL");
    response.setAccountDescription("Individual Brokerage");
    response.setAccountMode("MARGIN");

    CashBalance cash = new CashBalance();
    cash.setCashBalance(10000.0);
    cash.setCashAvailable(9500.0);
    response.setCash(cash);

    MarginBalance margin = new MarginBalance();
    margin.setMarginBalance(20000.0);
    margin.setMarginAvailable(19000.0);
    response.setMargin(margin);

    ComputedBalance computed = new ComputedBalance();
    computed.setTotal(30000.0);
    computed.setNetValue(29000.0);
    response.setComputed(computed);

    assertEquals("account123", response.getAccountId());
    assertEquals("INDIVIDUAL", response.getAccountType());
    assertEquals("Individual Brokerage", response.getAccountDescription());
    assertEquals("MARGIN", response.getAccountMode());
    assertNotNull(response.getCash());
    assertEquals(10000.0, response.getCash().getCashBalance());
    assertNotNull(response.getMargin());
    assertEquals(20000.0, response.getMargin().getMarginBalance());
    assertNotNull(response.getComputed());
    assertEquals(30000.0, response.getComputed().getTotal());
  }

  @Test
  @DisplayName("Should create BalanceResponse with default constructor")
  void shouldCreateWithDefaultConstructor() {
    BalanceResponse response = new BalanceResponse();

    assertNull(response.getAccountId());
    assertNull(response.getCash());
    assertNull(response.getMargin());
    assertNull(response.getComputed());
  }
}
