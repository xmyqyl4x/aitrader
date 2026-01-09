package com.myqyl.aitradex.etrade.alerts.dto;

/**
 * Alert DTO for Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Represents a single alert in the alerts list.
 */
public class AlertDto {

  private Long id; // The numeric alert ID (int64)
  private Long createTime; // The date and time the alert was issued, in Epoch time (int64)
  private String subject; // The subject of the alert
  private String status; // The current status of the alert (UNREAD, READ, DELETED, UNDELETED)

  public AlertDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
