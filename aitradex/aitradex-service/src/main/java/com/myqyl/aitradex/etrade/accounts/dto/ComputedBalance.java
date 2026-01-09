package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Computed balance section from BalanceResponse.
 */
public class ComputedBalance {

  private Double total;
  private Double netCash;
  private Double cashAvailableForInvestment;
  private Double totalValue;
  private Double netValue;
  private Double settledCash;
  private Double openCalls;
  private Double openPuts;

  public ComputedBalance() {
  }

  public Double getTotal() {
    return total;
  }

  public void setTotal(Double total) {
    this.total = total;
  }

  public Double getNetCash() {
    return netCash;
  }

  public void setNetCash(Double netCash) {
    this.netCash = netCash;
  }

  public Double getCashAvailableForInvestment() {
    return cashAvailableForInvestment;
  }

  public void setCashAvailableForInvestment(Double cashAvailableForInvestment) {
    this.cashAvailableForInvestment = cashAvailableForInvestment;
  }

  public Double getTotalValue() {
    return totalValue;
  }

  public void setTotalValue(Double totalValue) {
    this.totalValue = totalValue;
  }

  public Double getNetValue() {
    return netValue;
  }

  public void setNetValue(Double netValue) {
    this.netValue = netValue;
  }

  public Double getSettledCash() {
    return settledCash;
  }

  public void setSettledCash(Double settledCash) {
    this.settledCash = settledCash;
  }

  public Double getOpenCalls() {
    return openCalls;
  }

  public void setOpenCalls(Double openCalls) {
    this.openCalls = openCalls;
  }

  public Double getOpenPuts() {
    return openPuts;
  }

  public void setOpenPuts(Double openPuts) {
    this.openPuts = openPuts;
  }
}
