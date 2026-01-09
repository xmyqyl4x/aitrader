package com.myqyl.aitradex.etrade.alerts.dto;

/**
 * Failed Alert DTO from Delete Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Represents an alert that failed to delete.
 */
public class FailedAlertDto {

  private Long id; // The alert ID that failed to delete
  private String reason; // The reason for failure

  public FailedAlertDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
