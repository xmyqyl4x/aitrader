package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Revoke Access Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_consumer_key (header, required): The value used by the consumer to identify itself
 * - oauth_timestamp (header, required): The date and time of the request, in epoch time
 * - oauth_nonce (header, required): A nonce value
 * - oauth_signature_method (header, required): HMAC-SHA1
 * - oauth_signature (header, required): Signature generated with shared secret and token secret
 * - oauth_token (header, required): The consumer's access token to be revoked
 * 
 * This method revokes an access token that was granted for the consumer key.
 * Once revoked, it no longer grants access to E*TRADE data.
 */
public class RevokeAccessTokenRequest {

  @NotBlank(message = "OAuth access token is required")
  private String oauthToken;

  @NotBlank(message = "OAuth access token secret is required")
  private String oauthTokenSecret;

  public RevokeAccessTokenRequest() {
  }

  public RevokeAccessTokenRequest(String oauthToken, String oauthTokenSecret) {
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
