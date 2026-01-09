package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Get Access Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_token (string, required): The consumer's access token
 * - oauth_token_secret (string, required): The token secret
 * 
 * The production access token expires by default at midnight US Eastern time.
 * If the application does not make any requests for two hours, the access token is inactivated.
 */
public class AccessTokenResponse {

  @NotBlank(message = "OAuth access token is required")
  private String oauthToken;

  @NotBlank(message = "OAuth access token secret is required")
  private String oauthTokenSecret;

  public AccessTokenResponse() {
  }

  public AccessTokenResponse(String oauthToken, String oauthTokenSecret) {
    this.oauthToken = oauthToken;
    this.oauthTokenSecret = oauthTokenSecret;
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
}
