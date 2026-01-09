package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Totals DTO from Portfolio.
 * 
 * According to E*TRADE Accounts API documentation:
 * Provides aggregate totals for the portfolio.
 */
public class TotalsDto {

  private Double todaysGainLoss;
  private Double todaysGainLossPct;
  private Double totalGainLossPct;
  private Double totalMarketValue;
  private Double totalGainLoss;
  private Double totalPricePaid;
  private Double cashBalance;

  public TotalsDto() {
  }

  public Double getTodaysGainLoss() {
    return todaysGainLoss;
  }

  public void setTodaysGainLoss(Double todaysGainLoss) {
    this.todaysGainLoss = todaysGainLoss;
  }

  public Double getTodaysGainLossPct() {
    return todaysGainLossPct;
  }

  public void setTodaysGainLossPct(Double todaysGainLossPct) {
    this.todaysGainLossPct = todaysGainLossPct;
  }

  public Double getTotalGainLossPct() {
    return totalGainLossPct;
  }

  public void setTotalGainLossPct(Double totalGainLossPct) {
    this.totalGainLossPct = totalGainLossPct;
  }

  public Double getTotalMarketValue() {
    return totalMarketValue;
  }

  public void setTotalMarketValue(Double totalMarketValue) {
    this.totalMarketValue = totalMarketValue;
  }

  public Double getTotalGainLoss() {
    return totalGainLoss;
  }

  public void setTotalGainLoss(Double totalGainLoss) {
    this.totalGainLoss = totalGainLoss;
  }

  public Double getTotalPricePaid() {
    return totalPricePaid;
  }

  public void setTotalPricePaid(Double totalPricePaid) {
    this.totalPricePaid = totalPricePaid;
  }

  public Double getCashBalance() {
    return cashBalance;
  }

  public void setCashBalance(Double cashBalance) {
    this.cashBalance = cashBalance;
  }
}
