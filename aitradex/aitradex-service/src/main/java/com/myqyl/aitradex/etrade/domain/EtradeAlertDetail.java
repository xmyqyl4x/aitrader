package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity for etrade_alert_detail table.
 * 
 * Represents detailed information for an E*TRADE alert fetched from Get Alert Details API.
 * Linked to EtradeAlert via alert_id foreign key.
 */
@Entity
@Table(name = "etrade_alert_detail")
@EntityListeners(AuditingEntityListener.class)
public class EtradeAlertDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "alert_id", nullable = false)
  private UUID alertId; // Foreign key to etrade_alert.id

  @Column(name = "msg_text", columnDefinition = "text")
  private String msgText;

  @Column(name = "read_time")
  private Long readTime; // Epoch milliseconds

  @Column(name = "delete_time")
  private Long deleteTime; // Epoch milliseconds

  @Column(name = "symbol", length = 50)
  private String symbol;

  @Column(name = "next_url", length = 500)
  private String nextUrl;

  @Column(name = "prev_url", length = 500)
  private String prevUrl;

  @Column(name = "details_fetched_at")
  private OffsetDateTime detailsFetchedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // Getters and Setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAlertId() {
    return alertId;
  }

  public void setAlertId(UUID alertId) {
    this.alertId = alertId;
  }

  public String getMsgText() {
    return msgText;
  }

  public void setMsgText(String msgText) {
    this.msgText = msgText;
  }

  public Long getReadTime() {
    return readTime;
  }

  public void setReadTime(Long readTime) {
    this.readTime = readTime;
  }

  public Long getDeleteTime() {
    return deleteTime;
  }

  public void setDeleteTime(Long deleteTime) {
    this.deleteTime = deleteTime;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getNextUrl() {
    return nextUrl;
  }

  public void setNextUrl(String nextUrl) {
    this.nextUrl = nextUrl;
  }

  public String getPrevUrl() {
    return prevUrl;
  }

  public void setPrevUrl(String prevUrl) {
    this.prevUrl = prevUrl;
  }

  public OffsetDateTime getDetailsFetchedAt() {
    return detailsFetchedAt;
  }

  public void setDetailsFetchedAt(OffsetDateTime detailsFetchedAt) {
    this.detailsFetchedAt = detailsFetchedAt;
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
