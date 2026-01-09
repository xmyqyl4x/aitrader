package com.myqyl.aitradex.etrade.market.dto;

/**
 * Product Lookup DTO from Lookup Product API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains symbol, description, and type of found product.
 */
public class LookupProductDto {

  private String symbol;
  private String description;
  private String type; // EQUITY, OPTION, etc.

  public LookupProductDto() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
