package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Get Option Expire Dates API.
 * 
 * According to E*TRADE Market API documentation:
 * Query parameters:
 * - symbol (query, required): Stock symbol
 * - expiryType (query, optional): Expiry type filter (ALL, WEEKLY, MONTHLY)
 */
public class GetOptionExpireDatesRequest {

  @NotBlank(message = "Symbol is required")
  private String symbol;
  private String expiryType; // ALL, WEEKLY, MONTHLY

  public GetOptionExpireDatesRequest() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getExpiryType() {
    return expiryType;
  }

  public void setExpiryType(String expiryType) {
    this.expiryType = expiryType;
  }
}
