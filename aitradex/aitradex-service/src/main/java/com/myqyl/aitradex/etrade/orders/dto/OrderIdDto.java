package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OrderId DTO from Place Order Response.
 * 
 * According to E*TRADE Order API documentation:
 * Contains the order ID returned after placing an order.
 */
public class OrderIdDto {

  @NotBlank(message = "Order ID is required")
  private String orderId;

  public OrderIdDto() {
  }

  public OrderIdDto(String orderId) {
    this.orderId = orderId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
