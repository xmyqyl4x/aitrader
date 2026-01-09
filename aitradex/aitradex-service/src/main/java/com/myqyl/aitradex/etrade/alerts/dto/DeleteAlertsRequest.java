package com.myqyl.aitradex.etrade.alerts.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for Delete Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Path parameters:
 * - alert_id_list (path, required): Comma separated alertId list
 */
public class DeleteAlertsRequest {

  @NotNull(message = "Alert IDs are required")
  @NotEmpty(message = "At least one alert ID must be provided")
  private List<String> alertIds; // Comma-separated alert ID list

  public DeleteAlertsRequest() {
    this.alertIds = new ArrayList<>();
  }

  public List<String> getAlertIds() {
    return alertIds;
  }

  public void setAlertIds(List<String> alertIds) {
    this.alertIds = alertIds != null ? alertIds : new ArrayList<>();
  }

  /**
   * Converts the list of alert IDs to a comma-separated string for the path parameter.
   */
  public String toPathParameter() {
    return String.join(",", alertIds);
  }
}
