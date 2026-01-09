package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * QuickView DTO from Position.
 * 
 * According to E*TRADE Accounts API documentation:
 * Provides quick quote information for a position.
 */
public class QuickViewDto {

  private Double lastTrade;
  private Long lastTradeTime; // int64
  private Double change;
  private Double changePct;
  private Long volume; // int64
  private String quoteStatus; // REALTIME, DELAYED, CLOSING, EH_REALTIME, EH_BEFORE_OPEN, EH_CLOSED
  private Double sevenDayCurrentYield; // double
  private Double annualTotalReturn; // double
  private Double weightedAverageMaturity; // double

  public QuickViewDto() {
  }

  public Double getLastTrade() {
    return lastTrade;
  }

  public void setLastTrade(Double lastTrade) {
    this.lastTrade = lastTrade;
  }

  public Long getLastTradeTime() {
    return lastTradeTime;
  }

  public void setLastTradeTime(Long lastTradeTime) {
    this.lastTradeTime = lastTradeTime;
  }

  public Double getChange() {
    return change;
  }

  public void setChange(Double change) {
    this.change = change;
  }

  public Double getChangePct() {
    return changePct;
  }

  public void setChangePct(Double changePct) {
    this.changePct = changePct;
  }

  public Long getVolume() {
    return volume;
  }

  public void setVolume(Long volume) {
    this.volume = volume;
  }

  public String getQuoteStatus() {
    return quoteStatus;
  }

  public void setQuoteStatus(String quoteStatus) {
    this.quoteStatus = quoteStatus;
  }

  public Double getSevenDayCurrentYield() {
    return sevenDayCurrentYield;
  }

  public void setSevenDayCurrentYield(Double sevenDayCurrentYield) {
    this.sevenDayCurrentYield = sevenDayCurrentYield;
  }

  public Double getAnnualTotalReturn() {
    return annualTotalReturn;
  }

  public void setAnnualTotalReturn(Double annualTotalReturn) {
    this.annualTotalReturn = annualTotalReturn;
  }

  public Double getWeightedAverageMaturity() {
    return weightedAverageMaturity;
  }

  public void setWeightedAverageMaturity(Double weightedAverageMaturity) {
    this.weightedAverageMaturity = weightedAverageMaturity;
  }
}
