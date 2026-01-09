package com.myqyl.aitradex.etrade.alerts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for Get Alert Details API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Path parameters:
 * - id (path, required): The alert ID value. Alert id whose details are needed
 * Query parameters:
 * - tags (query, optional): The HTML tags on the alert. By default it is false. If set to true, it returns the alert details msgText with html tags.
 */
public class GetAlertDetailsRequest {

  @NotBlank(message = "Alert ID is required")
  @Positive(message = "Alert ID must be greater than 0")
  private String id; // Alert ID from path

  private Boolean tags; // HTML tags flag (default: false)

  public GetAlertDetailsRequest() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Boolean getTags() {
    return tags;
  }

  public void setTags(Boolean tags) {
    this.tags = tags;
  }
}
