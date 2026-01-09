package com.myqyl.aitradex.etrade.alerts.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Delete Alerts API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Contains result (Success) and optionally FailedAlerts array.
 */
public class DeleteAlertsResponse {

  private String result; // Success or failure result

  @Valid
  private List<FailedAlertDto> failedAlerts; // Array of alerts that failed to delete

  public DeleteAlertsResponse() {
    this.failedAlerts = new ArrayList<>();
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public List<FailedAlertDto> getFailedAlerts() {
    return failedAlerts;
  }

  public void setFailedAlerts(List<FailedAlertDto> failedAlerts) {
    this.failedAlerts = failedAlerts != null ? failedAlerts : new ArrayList<>();
  }

  /**
   * Returns true if the deletion was successful.
   */
  public boolean isSuccess() {
    return "Success".equalsIgnoreCase(result);
  }
}
