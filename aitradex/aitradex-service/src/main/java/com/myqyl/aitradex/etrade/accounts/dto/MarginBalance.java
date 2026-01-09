package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Margin balance section from BalanceResponse.
 */
public class MarginBalance {

  private Double marginBalance;
  private Double marginAvailable;
  private Double marginBuyingPower;
  private Double dayTradingBuyingPower;

  public MarginBalance() {
  }

  public Double getMarginBalance() {
    return marginBalance;
  }

  public void setMarginBalance(Double marginBalance) {
    this.marginBalance = marginBalance;
  }

  public Double getMarginAvailable() {
    return marginAvailable;
  }

  public void setMarginAvailable(Double marginAvailable) {
    this.marginAvailable = marginAvailable;
  }

  public Double getMarginBuyingPower() {
    return marginBuyingPower;
  }

  public void setMarginBuyingPower(Double marginBuyingPower) {
    this.marginBuyingPower = marginBuyingPower;
  }

  public Double getDayTradingBuyingPower() {
    return dayTradingBuyingPower;
  }

  public void setDayTradingBuyingPower(Double dayTradingBuyingPower) {
    this.dayTradingBuyingPower = dayTradingBuyingPower;
  }
}
