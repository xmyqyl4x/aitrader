package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PortfolioRequest DTO.
 */
@DisplayName("PortfolioRequest DTO Tests")
class PortfolioRequestTest {

  @Test
  @DisplayName("Should create PortfolioRequest with default constructor")
  void shouldCreateWithDefaultConstructor() {
    PortfolioRequest request = new PortfolioRequest();

    assertNull(request.getCount());
    assertNull(request.getSortBy());
    assertNull(request.getSortOrder());
    assertNull(request.getPageNumber());
    assertNull(request.getMarketSession());
    assertNull(request.getTotalsRequired());
    assertNull(request.getLotsRequired());
    assertNull(request.getView());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    PortfolioRequest request = new PortfolioRequest();

    request.setCount(25);
    request.setSortBy("SYMBOL");
    request.setSortOrder("ASC");
    request.setPageNumber(1);
    request.setMarketSession("REGULAR");
    request.setTotalsRequired(true);
    request.setLotsRequired(false);
    request.setView("QUICK");

    assertEquals(Integer.valueOf(25), request.getCount());
    assertEquals("SYMBOL", request.getSortBy());
    assertEquals("ASC", request.getSortOrder());
    assertEquals(Integer.valueOf(1), request.getPageNumber());
    assertEquals("REGULAR", request.getMarketSession());
    assertTrue(request.getTotalsRequired());
    assertFalse(request.getLotsRequired());
    assertEquals("QUICK", request.getView());
  }
}
