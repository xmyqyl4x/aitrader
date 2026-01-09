package com.myqyl.aitradex.etrade.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountPortfolio DTO from PortfolioResponse.
 * 
 * According to E*TRADE Accounts API documentation:
 * Contains positions for a single account within the portfolio response.
 */
public class AccountPortfolioDto {

  @NotBlank(message = "Account ID is required")
  private String accountId;

  private List<PositionDto> positions;
  private Integer totalPages;
  private TotalsDto totals;

  public AccountPortfolioDto() {
    this.positions = new ArrayList<>();
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<PositionDto> getPositions() {
    return positions;
  }

  public void setPositions(List<PositionDto> positions) {
    this.positions = positions != null ? positions : new ArrayList<>();
  }

  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public TotalsDto getTotals() {
    return totals;
  }

  public void setTotals(TotalsDto totals) {
    this.totals = totals;
  }
}
