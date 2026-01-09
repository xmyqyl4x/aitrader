package com.myqyl.aitradex.etrade.orders.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Order model DTO for E*TRADE Order API responses.
 * 
 * According to E*TRADE Order API documentation:
 * Represents a single order with all its details.
 * 
 * Note: This is different from com.myqyl.aitradex.api.dto.OrderDto which is for internal order management.
 */
public class EtradeOrderModel {

  private String orderId;
  private String orderType; // EQ, OPT, MF
  private String orderStatus; // OPEN, EXECUTED, CANCELLED, etc.
  private String accountId;
  private String clientOrderId;
  private Long placedTime; // int64 - epoch milliseconds
  private List<OrderDetailDto> orderDetails;
  private String marker; // For pagination

  public EtradeOrderModel() {
    this.orderDetails = new ArrayList<>();
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getOrderType() {
    return orderType;
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getClientOrderId() {
    return clientOrderId;
  }

  public void setClientOrderId(String clientOrderId) {
    this.clientOrderId = clientOrderId;
  }

  public Long getPlacedTime() {
    return placedTime;
  }

  public void setPlacedTime(Long placedTime) {
    this.placedTime = placedTime;
  }

  public List<OrderDetailDto> getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(List<OrderDetailDto> orderDetails) {
    this.orderDetails = orderDetails != null ? orderDetails : new ArrayList<>();
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(String marker) {
    this.marker = marker;
  }
}
