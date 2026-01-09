package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Cancel Order API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains success status and any messages.
 */
public class CancelOrderResponse {

  private Boolean success;
  
  @Valid
  private List<MessageDto> messages;

  public CancelOrderResponse() {
    this.success = true;
    this.messages = new ArrayList<>();
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success != null ? success : true;
  }

  public List<MessageDto> getMessages() {
    return messages;
  }

  public void setMessages(List<MessageDto> messages) {
    this.messages = messages != null ? messages : new ArrayList<>();
  }
}
