package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Response DTO for Authorize Application API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - Status Code 302: Redirect URL for Authorization
 * - oauth_verifier (string, uri): Verification code returned via callback URL or displayed on page
 * 
 * The authorization URL is returned, and the verifier is obtained either:
 * 1. Via callback URL parameter (oauth_verifier)
 * 2. Manual copy/paste from authorization page
 */
public class AuthorizeApplicationResponse {

  @NotBlank(message = "Authorization URL is required")
  private String authorizationUrl;

  /**
   * Verification code (available after user authorizes, either via callback or manual entry).
   * This field is populated after authorization is complete.
   */
  private String oauthVerifier;

  public AuthorizeApplicationResponse() {
  }

  public AuthorizeApplicationResponse(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public AuthorizeApplicationResponse(String authorizationUrl, String oauthVerifier) {
    this.authorizationUrl = authorizationUrl;
    this.oauthVerifier = oauthVerifier;
  }

  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public void setAuthorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public String getOauthVerifier() {
    return oauthVerifier;
  }

  public void setOauthVerifier(String oauthVerifier) {
    this.oauthVerifier = oauthVerifier;
  }
}
