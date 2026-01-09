package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Option Chain Snapshot entity.
 * 
 * Represents a snapshot of option chains for a symbol and expiration date.
 * Append-only snapshot per (symbol, expiry, requestTime).
 */
@Entity
@Table(name = "etrade_option_chain_snapshot")
@EntityListeners(AuditingEntityListener.class)
public class EtradeOptionChainSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "symbol", nullable = false)
  private String symbol;

  @Column(name = "expiry_year")
  private Integer expiryYear;

  @Column(name = "expiry_month")
  private Integer expiryMonth;

  @Column(name = "expiry_day")
  private Integer expiryDay;

  @Column(name = "near_price", precision = 19, scale = 8)
  private BigDecimal nearPrice;

  @Column(name = "adjusted_flag")
  private Boolean adjustedFlag;

  @Column(name = "option_chain_type", length = 50)
  private String optionChainType; // CALL, PUT, CALLPUT

  @Column(name = "quote_type", length = 50)
  private String quoteType;

  @Column(name = "timestamp")
  private Long timestamp; // E*TRADE timestamp

  @Column(name = "request_time", nullable = false)
  private OffsetDateTime requestTime;

  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public EtradeOptionChainSnapshot() {
    this.requestTime = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryDay() {
    return expiryDay;
  }

  public void setExpiryDay(Integer expiryDay) {
    this.expiryDay = expiryDay;
  }

  public BigDecimal getNearPrice() {
    return nearPrice;
  }

  public void setNearPrice(BigDecimal nearPrice) {
    this.nearPrice = nearPrice;
  }

  public Boolean getAdjustedFlag() {
    return adjustedFlag;
  }

  public void setAdjustedFlag(Boolean adjustedFlag) {
    this.adjustedFlag = adjustedFlag;
  }

  public String getOptionChainType() {
    return optionChainType;
  }

  public void setOptionChainType(String optionChainType) {
    this.optionChainType = optionChainType;
  }

  public String getQuoteType() {
    return quoteType;
  }

  public void setQuoteType(String quoteType) {
    this.quoteType = quoteType;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public OffsetDateTime getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(OffsetDateTime requestTime) {
    this.requestTime = requestTime;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
