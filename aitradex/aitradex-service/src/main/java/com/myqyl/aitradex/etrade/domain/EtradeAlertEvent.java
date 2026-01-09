package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity for etrade_alert_event table.
 * 
 * Represents audit/event log entries for alert operations (delete attempts, failures, sync runs).
 * Linked to EtradeAlert via alert_id foreign key.
 */
@Entity
@Table(name = "etrade_alert_event")
@EntityListeners(AuditingEntityListener.class)
public class EtradeAlertEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "alert_id", nullable = false)
  private UUID alertId; // Foreign key to etrade_alert.id

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType; // DELETE_ATTEMPT, DELETE_SUCCESS, DELETE_FAILURE, SYNC_RUN, etc.

  @Column(name = "event_status", length = 50)
  private String eventStatus; // SUCCESS, FAILURE, PENDING

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "event_data", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String eventData; // Additional event data stored as JSON

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

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

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEventStatus() {
    return eventStatus;
  }

  public void setEventStatus(String eventStatus) {
    this.eventStatus = eventStatus;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getEventData() {
    return eventData;
  }

  public void setEventData(String eventData) {
    this.eventData = eventData;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
