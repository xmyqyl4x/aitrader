package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Get Request Token API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_consumer_key (header, required): The value used by the consumer to identify itself
 * - oauth_timestamp (header, required): The date and time of the request, in epoch time
 * - oauth_nonce (header, required): A nonce value
 * - oauth_signature_method (header, required): HMAC-SHA1
 * - oauth_signature (header, required): Signature generated with shared secret
 * - oauth_callback (header, required): Must always be set to 'oob', whether using callback or not
 */
public class RequestTokenRequest {

  @NotBlank(message = "OAuth callback is required")
  private String oauthCallback;

  public RequestTokenRequest() {
  }

  public RequestTokenRequest(String oauthCallback) {
    this.oauthCallback = oauthCallback;
  }

  public String getOauthCallback() {
    return oauthCallback;
  }

  public void setOauthCallback(String oauthCallback) {
    this.oauthCallback = oauthCallback;
  }
}
