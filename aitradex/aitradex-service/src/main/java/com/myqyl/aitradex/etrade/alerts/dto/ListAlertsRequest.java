package com.myqyl.aitradex.etrade.alerts.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for List Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Query parameters:
 * - count (query, optional): The alert count. By default it returns 25. Max values that can be returned: 300.
 * - category (query, optional): The alert category. By default it will return STOCK and ACCOUNT. (STOCK, ACCOUNT)
 * - status (query, optional): The alert status. By default it will return READ and UNREAD. (READ, UNREAD, DELETED)
 * - direction (query, optional): Sorting is done based on the createDate (ASC, DESC)
 * - search (query, optional): The alert search. Search is done based on the subject.
 */
public class ListAlertsRequest {

  @Min(value = 1, message = "Count must be at least 1")
  @Max(value = 300, message = "Count cannot exceed 300")
  private Integer count; // Default: 25, Max: 300

  private String category; // STOCK, ACCOUNT (default: both)

  private String status; // READ, UNREAD, DELETED (default: READ and UNREAD)

  private String direction; // ASC, DESC

  private String search; // Search term based on subject

  public ListAlertsRequest() {
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }
}
