package com.myqyl.aitradex.etrade.market.dto;

/**
 * Product DTO for Quote.
 * 
 * According to E*TRADE Market API documentation:
 * Represents the security/product information in a quote.
 */
public class QuoteProductDto {

  private String symbol;
  private String exchange;
  private String companyName;
  private String securityType;

  public QuoteProductDto() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getSecurityType() {
    return securityType;
  }

  public void setSecurityType(String securityType) {
    this.securityType = securityType;
  }
}
