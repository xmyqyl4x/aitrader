package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Cash balance section from BalanceResponse.
 */
public class CashBalance {

  private Double cashBalance;
  private Double cashAvailable;
  private Double unclearedDeposits;
  private Double cashSweep;

  public CashBalance() {
  }

  public Double getCashBalance() {
    return cashBalance;
  }

  public void setCashBalance(Double cashBalance) {
    this.cashBalance = cashBalance;
  }

  public Double getCashAvailable() {
    return cashAvailable;
  }

  public void setCashAvailable(Double cashAvailable) {
    this.cashAvailable = cashAvailable;
  }

  public Double getUnclearedDeposits() {
    return unclearedDeposits;
  }

  public void setUnclearedDeposits(Double unclearedDeposits) {
    this.unclearedDeposits = unclearedDeposits;
  }

  public Double getCashSweep() {
    return cashSweep;
  }

  public void setCashSweep(Double cashSweep) {
    this.cashSweep = cashSweep;
  }
}
