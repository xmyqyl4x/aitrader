package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Get Option Chains API.
 * 
 * According to E*TRADE Market API documentation:
 * Query parameters:
 * - symbol (query, required): Stock symbol
 * - expiryYear (query, optional): Expiration year
 * - expiryMonth (query, optional): Expiration month (1-12)
 * - expiryDay (query, optional): Expiration day
 * - strikePriceNear (query, optional): Strike price near this value
 * - noOfStrikes (query, optional): Number of strikes above and below
 * - optionCategory (query, optional): STANDARD, MINI, etc.
 * - chainType (query, optional): Type of option chain
 */
public class GetOptionChainsRequest {

  @NotBlank(message = "Symbol is required")
  private String symbol;
  private Integer expiryYear;
  private Integer expiryMonth;
  private Integer expiryDay;
  private Integer strikePriceNear;
  private Integer noOfStrikes;
  private String optionCategory;
  private String chainType;

  public GetOptionChainsRequest() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryDay() {
    return expiryDay;
  }

  public void setExpiryDay(Integer expiryDay) {
    this.expiryDay = expiryDay;
  }

  public Integer getStrikePriceNear() {
    return strikePriceNear;
  }

  public void setStrikePriceNear(Integer strikePriceNear) {
    this.strikePriceNear = strikePriceNear;
  }

  public Integer getNoOfStrikes() {
    return noOfStrikes;
  }

  public void setNoOfStrikes(Integer noOfStrikes) {
    this.noOfStrikes = noOfStrikes;
  }

  public String getOptionCategory() {
    return optionCategory;
  }

  public void setOptionCategory(String optionCategory) {
    this.optionCategory = optionCategory;
  }

  public String getChainType() {
    return chainType;
  }

  public void setChainType(String chainType) {
    this.chainType = chainType;
  }
}
