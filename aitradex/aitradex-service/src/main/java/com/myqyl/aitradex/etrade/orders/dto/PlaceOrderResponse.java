package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Place Order API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains order IDs and any messages.
 */
public class PlaceOrderResponse {

  @Valid
  private List<OrderIdDto> orderIds;
  
  private Long placedTime; // Epoch milliseconds from E*TRADE
  
  @Valid
  private List<MessageDto> messages;

  public PlaceOrderResponse() {
    this.orderIds = new ArrayList<>();
    this.messages = new ArrayList<>();
  }

  public List<OrderIdDto> getOrderIds() {
    return orderIds;
  }

  public void setOrderIds(List<OrderIdDto> orderIds) {
    this.orderIds = orderIds != null ? orderIds : new ArrayList<>();
  }

  public Long getPlacedTime() {
    return placedTime;
  }

  public void setPlacedTime(Long placedTime) {
    this.placedTime = placedTime;
  }

  public List<MessageDto> getMessages() {
    return messages;
  }

  public void setMessages(List<MessageDto> messages) {
    this.messages = messages != null ? messages : new ArrayList<>();
  }
}
