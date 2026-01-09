package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Preview Order API.
 * 
 * According to E*TRADE Order API documentation:
 * Contains preview IDs, order details, and cost estimates.
 */
public class PreviewOrderResponse {

  private String accountId;
  
  @Valid
  private List<PreviewIdDto> previewIds;
  
  @Valid
  private List<EtradeOrderModel> orders;
  
  private Double totalOrderValue;
  private Double estimatedCommission;
  private Double estimatedTotalAmount;
  
  @Valid
  private List<MessageDto> messages;

  public PreviewOrderResponse() {
    this.previewIds = new ArrayList<>();
    this.orders = new ArrayList<>();
    this.messages = new ArrayList<>();
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<PreviewIdDto> getPreviewIds() {
    return previewIds;
  }

  public void setPreviewIds(List<PreviewIdDto> previewIds) {
    this.previewIds = previewIds != null ? previewIds : new ArrayList<>();
  }

  public List<EtradeOrderModel> getOrders() {
    return orders;
  }

  public void setOrders(List<EtradeOrderModel> orders) {
    this.orders = orders != null ? orders : new ArrayList<>();
  }

  public Double getTotalOrderValue() {
    return totalOrderValue;
  }

  public void setTotalOrderValue(Double totalOrderValue) {
    this.totalOrderValue = totalOrderValue;
  }

  public Double getEstimatedCommission() {
    return estimatedCommission;
  }

  public void setEstimatedCommission(Double estimatedCommission) {
    this.estimatedCommission = estimatedCommission;
  }

  public Double getEstimatedTotalAmount() {
    return estimatedTotalAmount;
  }

  public void setEstimatedTotalAmount(Double estimatedTotalAmount) {
    this.estimatedTotalAmount = estimatedTotalAmount;
  }

  public List<MessageDto> getMessages() {
    return messages;
  }

  public void setMessages(List<MessageDto> messages) {
    this.messages = messages != null ? messages : new ArrayList<>();
  }
}
