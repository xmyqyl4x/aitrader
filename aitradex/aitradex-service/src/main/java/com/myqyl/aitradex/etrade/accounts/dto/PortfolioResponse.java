package com.myqyl.aitradex.etrade.accounts.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for View Portfolio API.
 * 
 * According to E*TRADE Accounts API documentation:
 * Contains account portfolio information including positions, totals, and pagination.
 */
public class PortfolioResponse {

  @Valid
  private List<AccountPortfolioDto> accountPortfolios;
  private Integer totalPages;

  public PortfolioResponse() {
    this.accountPortfolios = new ArrayList<>();
  }

  public PortfolioResponse(List<AccountPortfolioDto> accountPortfolios) {
    this.accountPortfolios = accountPortfolios != null ? accountPortfolios : new ArrayList<>();
  }

  public List<AccountPortfolioDto> getAccountPortfolios() {
    return accountPortfolios;
  }

  public void setAccountPortfolios(List<AccountPortfolioDto> accountPortfolios) {
    this.accountPortfolios = accountPortfolios != null ? accountPortfolios : new ArrayList<>();
  }

  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  /**
   * Helper method to get all positions from all account portfolios.
   */
  public List<PositionDto> getAllPositions() {
    List<PositionDto> allPositions = new ArrayList<>();
    for (AccountPortfolioDto accountPortfolio : accountPortfolios) {
      if (accountPortfolio.getPositions() != null) {
        allPositions.addAll(accountPortfolio.getPositions());
      }
    }
    return allPositions;
  }

  /**
   * Helper method to get first account ID.
   */
  public String getAccountId() {
    if (accountPortfolios != null && !accountPortfolios.isEmpty()) {
      return accountPortfolios.get(0).getAccountId();
    }
    return null;
  }
}
