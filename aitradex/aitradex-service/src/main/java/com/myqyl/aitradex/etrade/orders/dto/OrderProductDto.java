package com.myqyl.aitradex.etrade.orders.dto;

/**
 * Product DTO for Order Instrument.
 * 
 * According to E*TRADE Order API documentation:
 * Represents the security/product being traded in an order.
 */
public class OrderProductDto {

  private String symbol;
  private String securityType; // EQ, OPTN, MF, MMF, etc.
  private String symbolDescription;
  private String cusip;
  private String exchange;

  public OrderProductDto() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getSecurityType() {
    return securityType;
  }

  public void setSecurityType(String securityType) {
    this.securityType = securityType;
  }

  public String getSymbolDescription() {
    return symbolDescription;
  }

  public void setSymbolDescription(String symbolDescription) {
    this.symbolDescription = symbolDescription;
  }

  public String getCusip() {
    return cusip;
  }

  public void setCusip(String cusip) {
    this.cusip = cusip;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }
}
