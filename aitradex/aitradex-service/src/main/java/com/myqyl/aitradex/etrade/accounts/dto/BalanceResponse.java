package com.myqyl.aitradex.etrade.accounts.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Get Account Balances API.
 * 
 * According to E*TRADE Accounts API documentation:
 * Contains account information and balance details including Cash, Margin, and Computed sections.
 */
public class BalanceResponse {

  @NotBlank(message = "Account ID is required")
  private String accountId;

  private String accountType;
  private String accountDescription;
  private String accountMode;

  private CashBalance cash;
  private MarginBalance margin;
  private ComputedBalance computed;

  public BalanceResponse() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public String getAccountDescription() {
    return accountDescription;
  }

  public void setAccountDescription(String accountDescription) {
    this.accountDescription = accountDescription;
  }

  public String getAccountMode() {
    return accountMode;
  }

  public void setAccountMode(String accountMode) {
    this.accountMode = accountMode;
  }

  public CashBalance getCash() {
    return cash;
  }

  public void setCash(CashBalance cash) {
    this.cash = cash;
  }

  public MarginBalance getMargin() {
    return margin;
  }

  public void setMargin(MarginBalance margin) {
    this.margin = margin;
  }

  public ComputedBalance getComputed() {
    return computed;
  }

  public void setComputed(ComputedBalance computed) {
    this.computed = computed;
  }
}
