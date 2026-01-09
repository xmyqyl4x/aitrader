package com.myqyl.aitradex.etrade.market.dto;

/**
 * Option Pair DTO for Option Chains.
 * 
 * According to E*TRADE Market API documentation:
 * Represents a pair of call and put options at the same strike price.
 */
public class OptionPairDto {

  private Double strikePrice;
  private OptionDto call;
  private OptionDto put;

  public OptionPairDto() {
  }

  public Double getStrikePrice() {
    return strikePrice;
  }

  public void setStrikePrice(Double strikePrice) {
    this.strikePrice = strikePrice;
  }

  public OptionDto getCall() {
    return call;
  }

  public void setCall(OptionDto call) {
    this.call = call;
  }

  public OptionDto getPut() {
    return put;
  }

  public void setPut(OptionDto put) {
    this.put = put;
  }
}
