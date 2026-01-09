package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Cancel Order API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains the order ID to cancel.
 */
public class CancelOrderRequest {

  @NotBlank(message = "Order ID is required")
  private String orderId;

  public CancelOrderRequest() {
  }

  public CancelOrderRequest(String orderId) {
    this.orderId = orderId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
