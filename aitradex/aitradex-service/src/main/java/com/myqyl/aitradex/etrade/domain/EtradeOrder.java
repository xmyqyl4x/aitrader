package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "etrade_order")
@EntityListeners(AuditingEntityListener.class)
public class EtradeOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "etrade_order_id")
  private String etradeOrderId;

  @Column(name = "symbol", nullable = false, length = 10)
  private String symbol;

  @Column(name = "order_type", nullable = false)
  private String orderType;

  @Column(name = "price_type", nullable = false)
  private String priceType;

  @Column(name = "side", nullable = false, length = 10)
  private String side; // BUY or SELL

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "limit_price", precision = 19, scale = 8)
  private BigDecimal limitPrice;

  @Column(name = "stop_price", precision = 19, scale = 8)
  private BigDecimal stopPrice;

  @Column(name = "order_status")
  private String orderStatus;

  @Column(name = "placed_at")
  private OffsetDateTime placedAt;

  @Column(name = "executed_at")
  private OffsetDateTime executedAt;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @Column(name = "preview_data", columnDefinition = "jsonb")
  private String previewData;

  @Column(name = "order_response", columnDefinition = "jsonb")
  private String orderResponse;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // Constructors
  public EtradeOrder() {
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public String getEtradeOrderId() {
    return etradeOrderId;
  }

  public void setEtradeOrderId(String etradeOrderId) {
    this.etradeOrderId = etradeOrderId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getOrderType() {
    return orderType;
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public String getPriceType() {
    return priceType;
  }

  public void setPriceType(String priceType) {
    this.priceType = priceType;
  }

  public String getSide() {
    return side;
  }

  public void setSide(String side) {
    this.side = side;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(BigDecimal limitPrice) {
    this.limitPrice = limitPrice;
  }

  public BigDecimal getStopPrice() {
    return stopPrice;
  }

  public void setStopPrice(BigDecimal stopPrice) {
    this.stopPrice = stopPrice;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  public OffsetDateTime getPlacedAt() {
    return placedAt;
  }

  public void setPlacedAt(OffsetDateTime placedAt) {
    this.placedAt = placedAt;
  }

  public OffsetDateTime getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(OffsetDateTime executedAt) {
    this.executedAt = executedAt;
  }

  public OffsetDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public String getPreviewData() {
    return previewData;
  }

  public void setPreviewData(String previewData) {
    this.previewData = previewData;
  }

  public String getOrderResponse() {
    return orderResponse;
  }

  public void setOrderResponse(String orderResponse) {
    this.orderResponse = orderResponse;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
