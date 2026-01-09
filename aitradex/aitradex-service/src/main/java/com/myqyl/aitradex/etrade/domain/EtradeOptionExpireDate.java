package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Option Expire Date entity.
 * 
 * Represents an expiration date for options on a symbol.
 * Upserted by (symbol, expiryYear, expiryMonth, expiryDay) combination.
 */
@Entity
@Table(name = "etrade_option_expire_date", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "expiry_year", "expiry_month", "expiry_day"})
})
@EntityListeners(AuditingEntityListener.class)
public class EtradeOptionExpireDate {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "symbol", nullable = false)
  private String symbol;

  @Column(name = "expiry_year", nullable = false)
  private Integer expiryYear;

  @Column(name = "expiry_month", nullable = false)
  private Integer expiryMonth;

  @Column(name = "expiry_day", nullable = false)
  private Integer expiryDay;

  @Column(name = "expiry_type", length = 50)
  private String expiryType; // DAILY, WEEKLY, MONTHLY, ALL

  @Column(name = "last_synced_at", nullable = false)
  private OffsetDateTime lastSyncedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public EtradeOptionExpireDate() {
    this.lastSyncedAt = OffsetDateTime.now();
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

  public String getExpiryType() {
    return expiryType;
  }

  public void setExpiryType(String expiryType) {
    this.expiryType = expiryType;
  }

  public OffsetDateTime getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
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
