package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for Place Order API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains preview ID, order type, client order ID, and order details.
 */
public class PlaceOrderRequest {

  @NotBlank(message = "Preview ID is required")
  private String previewId; // From PreviewOrderResponse

  @NotBlank(message = "Order type is required")
  private String orderType; // EQ, OPT, MF

  private String clientOrderId;

  @NotNull(message = "Orders list is required")
  @Valid
  private List<OrderDetailDto> orders;

  public PlaceOrderRequest() {
    this.orders = new ArrayList<>();
  }

  public String getPreviewId() {
    return previewId;
  }

  public void setPreviewId(String previewId) {
    this.previewId = previewId;
  }

  public String getOrderType() {
    return orderType;
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public String getClientOrderId() {
    return clientOrderId;
  }

  public void setClientOrderId(String clientOrderId) {
    this.clientOrderId = clientOrderId;
  }

  public List<OrderDetailDto> getOrders() {
    return orders;
  }

  public void setOrders(List<OrderDetailDto> orders) {
    this.orders = orders != null ? orders : new ArrayList<>();
  }
}
