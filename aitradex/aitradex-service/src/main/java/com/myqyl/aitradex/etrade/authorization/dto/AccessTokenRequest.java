package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Get Access Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_consumer_key (header, required): The value used by the consumer to identify itself
 * - oauth_timestamp (header, required): The date and time of the request, in epoch time
 * - oauth_nonce (header, required): A nonce value
 * - oauth_signature_method (header, required): HMAC-SHA1
 * - oauth_signature (header, required): Signature generated with shared secret and token secret
 * - oauth_token (header, required): The consumer's request token to be exchanged for an access token
 * - oauth_verifier (header, required): The verification code received by the user
 */
public class AccessTokenRequest {

  @NotBlank(message = "OAuth request token is required")
  private String oauthToken;

  @NotBlank(message = "OAuth request token secret is required")
  private String oauthTokenSecret;

  @NotBlank(message = "OAuth verifier is required")
  private String oauthVerifier;

  public AccessTokenRequest() {
  }

  public AccessTokenRequest(String oauthToken, String oauthTokenSecret, String oauthVerifier) {
    this.oauthToken = oauthToken;
    this.oauthTokenSecret = oauthTokenSecret;
    this.oauthVerifier = oauthVerifier;
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

  public String getOauthVerifier() {
    return oauthVerifier;
  }

  public void setOauthVerifier(String oauthVerifier) {
    this.oauthVerifier = oauthVerifier;
  }
}
