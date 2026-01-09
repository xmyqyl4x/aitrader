package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Get Request Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_token (string, required): The consumer's request token
 * - oauth_token_secret (string, required): The token secret
 * - oauth_callback_confirmed (string, required): Returns true if callback URL is configured, otherwise false
 * 
 * The request token expires after 5 minutes.
 */
public class RequestTokenResponse {

  @NotBlank(message = "OAuth token is required")
  private String oauthToken;

  @NotBlank(message = "OAuth token secret is required")
  private String oauthTokenSecret;

  @NotBlank(message = "OAuth callback confirmed is required")
  private String oauthCallbackConfirmed;

  public RequestTokenResponse() {
  }

  public RequestTokenResponse(String oauthToken, String oauthTokenSecret, String oauthCallbackConfirmed) {
    this.oauthToken = oauthToken;
    this.oauthTokenSecret = oauthTokenSecret;
    this.oauthCallbackConfirmed = oauthCallbackConfirmed;
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

  public String getOauthCallbackConfirmed() {
    return oauthCallbackConfirmed;
  }

  public void setOauthCallbackConfirmed(String oauthCallbackConfirmed) {
    this.oauthCallbackConfirmed = oauthCallbackConfirmed;
  }

  /**
   * Returns true if callback URL is configured, false otherwise.
   */
  public boolean isCallbackConfirmed() {
    return "true".equalsIgnoreCase(oauthCallbackConfirmed);
  }
}
