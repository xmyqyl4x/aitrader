package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for List Orders API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains list of orders and pagination information.
 */
public class OrdersResponse {

  @Valid
  private List<EtradeOrderModel> orders;
  private String marker; // For pagination
  private Boolean moreOrders; // Whether more orders are available

  public OrdersResponse() {
    this.orders = new ArrayList<>();
  }

  public List<EtradeOrderModel> getOrders() {
    return orders;
  }

  public void setOrders(List<EtradeOrderModel> orders) {
    this.orders = orders != null ? orders : new ArrayList<>();
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(String marker) {
    this.marker = marker;
  }

  public Boolean getMoreOrders() {
    return moreOrders;
  }

  public void setMoreOrders(Boolean moreOrders) {
    this.moreOrders = moreOrders;
  }
}
