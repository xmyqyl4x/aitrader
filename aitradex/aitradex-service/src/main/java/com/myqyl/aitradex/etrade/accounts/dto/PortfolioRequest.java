package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Request DTO for View Portfolio API.
 * 
 * According to E*TRADE Accounts API documentation:
 * Query parameters (all optional):
 * - count: Number of positions to return
 * - sortBy: Sort field (e.g., "SYMBOL", "QUANTITY", "MARKET_VALUE")
 * - sortOrder: Sort direction ("ASC", "DESC")
 * - pageNumber: Page number for pagination
 * - marketSession: Market session filter
 * - totalsRequired: Whether to include totals
 * - lotsRequired: Whether to include lot details
 * - view: View type (e.g., "QUICK", "COMPLETE")
 */
public class PortfolioRequest {

  private Integer count;
  private String sortBy;
  private String sortOrder; // ASC, DESC
  private Integer pageNumber;
  private String marketSession;
  private Boolean totalsRequired;
  private Boolean lotsRequired;
  private String view; // QUICK, COMPLETE

  public PortfolioRequest() {
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Integer getPageNumber() {
    return pageNumber;
  }

  public void setPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
  }

  public String getMarketSession() {
    return marketSession;
  }

  public void setMarketSession(String marketSession) {
    this.marketSession = marketSession;
  }

  public Boolean getTotalsRequired() {
    return totalsRequired;
  }

  public void setTotalsRequired(Boolean totalsRequired) {
    this.totalsRequired = totalsRequired;
  }

  public Boolean getLotsRequired() {
    return lotsRequired;
  }

  public void setLotsRequired(Boolean lotsRequired) {
    this.lotsRequired = lotsRequired;
  }

  public String getView() {
    return view;
  }

  public void setView(String view) {
    this.view = view;
  }
}
