package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PortfolioResponse DTO.
 */
@DisplayName("PortfolioResponse DTO Tests")
class PortfolioResponseTest {

  @Test
  @DisplayName("Should create PortfolioResponse with default constructor")
  void shouldCreateWithDefaultConstructor() {
    PortfolioResponse response = new PortfolioResponse();

    assertNotNull(response.getAccountPortfolios());
    assertTrue(response.getAccountPortfolios().isEmpty());
    assertNull(response.getTotalPages());
  }

  @Test
  @DisplayName("Should create PortfolioResponse with account portfolios")
  void shouldCreateWithAccountPortfolios() {
    List<AccountPortfolioDto> portfolios = new ArrayList<>();
    AccountPortfolioDto portfolio1 = new AccountPortfolioDto();
    portfolio1.setAccountId("account1");
    portfolios.add(portfolio1);

    PortfolioResponse response = new PortfolioResponse(portfolios);

    assertEquals(1, response.getAccountPortfolios().size());
    assertEquals("account1", response.getAccountPortfolios().get(0).getAccountId());
  }

  @Test
  @DisplayName("Should get all positions from all portfolios")
  void shouldGetAllPositions() {
    PortfolioResponse response = new PortfolioResponse();

    AccountPortfolioDto portfolio1 = new AccountPortfolioDto();
    PositionDto position1 = new PositionDto();
    ProductDto product1 = new ProductDto();
    product1.setSymbol("AAPL");
    position1.setProduct(product1);
    List<PositionDto> positions1 = new ArrayList<>();
    positions1.add(position1);
    portfolio1.setPositions(positions1);

    AccountPortfolioDto portfolio2 = new AccountPortfolioDto();
    PositionDto position2 = new PositionDto();
    ProductDto product2 = new ProductDto();
    product2.setSymbol("GOOGL");
    position2.setProduct(product2);
    List<PositionDto> positions2 = new ArrayList<>();
    positions2.add(position2);
    portfolio2.setPositions(positions2);

    List<AccountPortfolioDto> portfolios = new ArrayList<>();
    portfolios.add(portfolio1);
    portfolios.add(portfolio2);
    response.setAccountPortfolios(portfolios);

    List<PositionDto> allPositions = response.getAllPositions();
    assertEquals(2, allPositions.size());
    assertEquals("AAPL", allPositions.get(0).getProduct().getSymbol());
    assertEquals("GOOGL", allPositions.get(1).getProduct().getSymbol());
  }

  @Test
  @DisplayName("Should get first account ID")
  void shouldGetFirstAccountId() {
    PortfolioResponse response = new PortfolioResponse();

    AccountPortfolioDto portfolio1 = new AccountPortfolioDto();
    portfolio1.setAccountId("account1");
    List<AccountPortfolioDto> portfolios = new ArrayList<>();
    portfolios.add(portfolio1);
    response.setAccountPortfolios(portfolios);

    assertEquals("account1", response.getAccountId());
  }

  @Test
  @DisplayName("Should return null account ID when no portfolios")
  void shouldReturnNullAccountIdWhenNoPortfolios() {
    PortfolioResponse response = new PortfolioResponse();

    assertNull(response.getAccountId());
  }
}
