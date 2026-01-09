package com.myqyl.aitradex.etrade.orders.dto;

/**
 * Request DTO for List Orders API.
 * 
 * According to E*TRADE Order API documentation:
 * Query parameters:
 * - marker (query, optional): Pagination marker
 * - count (query, optional): Number of orders to return (max 100, default 25)
 * - status (query, optional): Order status filter
 * - fromDate (query, optional): Earliest date (MMDDYYYY format)
 * - toDate (query, optional): Latest date (MMDDYYYY format)
 * - symbol (query, optional): Symbol filter (up to 25 symbols, comma-separated)
 * - securityType (query, optional): Security type filter
 * - transactionType (query, optional): Transaction type filter (ATNM, BUY, SELL, etc.)
 * - marketSession (query, optional): Market session filter (REGULAR, EXTENDED)
 */
public class ListOrdersRequest {

  private String marker;
  private Integer count; // Max 100, default 25
  private String status;
  private String fromDate; // MMDDYYYY format
  private String toDate; // MMDDYYYY format
  private String symbol; // Up to 25 symbols, comma-separated
  private String securityType;
  private String transactionType; // ATNM, BUY, SELL, SELL_SHORT, BUY_TO_COVER, MF_EXCHANGE
  private String marketSession; // REGULAR, EXTENDED

  public ListOrdersRequest() {
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(String marker) {
    this.marker = marker;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFromDate() {
    return fromDate;
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public String getToDate() {
    return toDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
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

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }

  public String getMarketSession() {
    return marketSession;
  }

  public void setMarketSession(String marketSession) {
    this.marketSession = marketSession;
  }
}
