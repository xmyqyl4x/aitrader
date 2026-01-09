package com.myqyl.aitradex.etrade.alerts.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for List Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Contains totalAlerts count and array of Alert objects.
 */
public class AlertsResponse {

  private Long totalAlerts; // The total number of alerts for the user including READ, UNREAD and DELETED (int64)

  @Valid
  private List<AlertDto> alerts; // The array of alert responses

  public AlertsResponse() {
    this.alerts = new ArrayList<>();
  }

  public Long getTotalAlerts() {
    return totalAlerts;
  }

  public void setTotalAlerts(Long totalAlerts) {
    this.totalAlerts = totalAlerts;
  }

  public List<AlertDto> getAlerts() {
    return alerts;
  }

  public void setAlerts(List<AlertDto> alerts) {
    this.alerts = alerts != null ? alerts : new ArrayList<>();
  }
}
