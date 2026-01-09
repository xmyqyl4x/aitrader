package com.myqyl.aitradex.etrade.authorization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Authorize Application API.
 * 
 * According to E*TRADE Authorization API documentation:
 * - oauth_consumer_key (header, required): The value used by the consumer to identify itself
 * - oauth_token (header, required): The consumer's request token
 * 
 * Note: This is not a REST API call, but generates a URL to redirect the user to E*TRADE authorization page.
 */
public class AuthorizeApplicationRequest {

  @NotBlank(message = "OAuth consumer key is required")
  private String oauthConsumerKey;

  @NotBlank(message = "OAuth token is required")
  private String oauthToken;

  public AuthorizeApplicationRequest() {
  }

  public AuthorizeApplicationRequest(String oauthConsumerKey, String oauthToken) {
    this.oauthConsumerKey = oauthConsumerKey;
    this.oauthToken = oauthToken;
  }

  public String getOauthConsumerKey() {
    return oauthConsumerKey;
  }

  public void setOauthConsumerKey(String oauthConsumerKey) {
    this.oauthConsumerKey = oauthConsumerKey;
  }

  public String getOauthToken() {
    return oauthToken;
  }

  public void setOauthToken(String oauthToken) {
    this.oauthToken = oauthToken;
  }
}
