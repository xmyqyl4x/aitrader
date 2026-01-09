package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Renew Access Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - Response: "Access Token has been renewed" (string message)
 * 
 * Status Code 200 indicates successful renewal.
 */
public class RenewAccessTokenResponse {

  @NotBlank(message = "Response message is required")
  private String message;

  public RenewAccessTokenResponse() {
  }

  public RenewAccessTokenResponse(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Returns true if the renewal was successful.
   */
  public boolean isSuccess() {
    return message != null && message.toLowerCase().contains("renewed");
  }
}
