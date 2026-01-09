package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Revoke Access Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - Response: "Revoked Access Token" (string message)
 * 
 * Status Code 200 indicates successful revocation.
 * Note: The documentation shows oauth_token and oauth_token_secret in response model,
 * but the example response only shows the message string.
 */
public class RevokeAccessTokenResponse {

  @NotBlank(message = "Response message is required")
  private String message;

  private String oauthToken;
  private String oauthTokenSecret;

  public RevokeAccessTokenResponse() {
  }

  public RevokeAccessTokenResponse(String message) {
    this.message = message;
  }

  public RevokeAccessTokenResponse(String message, String oauthToken, String oauthTokenSecret) {
    this.message = message;
    this.oauthToken = oauthToken;
    this.oauthTokenSecret = oauthTokenSecret;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getOauthToken() {
    return oauthToken;
  }

  public void setOauthToken(String oauthToken) {
    this.oauthToken = oauthToken;
  }

  public String getOauthTokenSecret() {
    return oauthTokenSecret;
  }

  public void setOauthTokenSecret(String oauthTokenSecret) {
    this.oauthTokenSecret = oauthTokenSecret;
  }

  /**
   * Returns true if the revocation was successful.
   */
  public boolean isSuccess() {
    return message != null && message.toLowerCase().contains("revoked");
  }
}
