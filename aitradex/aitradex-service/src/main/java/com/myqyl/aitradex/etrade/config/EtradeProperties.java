package com.myqyl.aitradex.etrade.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for E*TRADE API integration.
 * Secrets should be provided via environment variables for security.
 */
@ConfigurationProperties(prefix = "app.etrade")
@Validated
public class EtradeProperties {

  @NotBlank
  private String consumerKey;

  @NotBlank
  private String consumerSecret;

  @NotBlank
  private String callbackUrl = "http://localhost:4200/etrade-review-trade/callback";

  @NotNull
  private Environment environment = Environment.SANDBOX;

  @NotBlank
  private String baseUrl = "https://apisb.etrade.com";

  @NotBlank
  private String authorizeUrl = "https://us.etrade.com/e/t/etws/authorize";

  private boolean enabled = true;

  // Encryption key for storing tokens (should be from environment variable)
  @NotBlank
  private String encryptionKey;

  public enum Environment {
    SANDBOX,
    PRODUCTION
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public String getConsumerSecret() {
    return consumerSecret;
  }

  public void setConsumerSecret(String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public void setCallbackUrl(String callbackUrl) {
    this.callbackUrl = callbackUrl;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
    // Auto-update base URL based on environment
    if (environment == Environment.PRODUCTION) {
      this.baseUrl = "https://api.etrade.com";
    } else {
      this.baseUrl = "https://apisb.etrade.com";
    }
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getAuthorizeUrl() {
    return authorizeUrl;
  }

  public void setAuthorizeUrl(String authorizeUrl) {
    this.authorizeUrl = authorizeUrl;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getEncryptionKey() {
    return encryptionKey;
  }

  public void setEncryptionKey(String encryptionKey) {
    this.encryptionKey = encryptionKey;
  }

  // API endpoints
  public String getOAuthRequestTokenUrl() {
    return baseUrl + "/oauth/request_token";
  }

  public String getOAuthAccessTokenUrl() {
    return baseUrl + "/oauth/access_token";
  }

  public String getAccountsListUrl() {
    return baseUrl + "/v1/accounts/list";
  }

  public String getBalanceUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/balance";
  }

  public String getPortfolioUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/portfolio";
  }

  public String getQuoteUrl() {
    return baseUrl + "/v1/market/quote";
  }

  public String getOrdersUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/orders";
  }

  public String getOrderPreviewUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/orders/preview";
  }

  public String getOrderPlaceUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/orders/place";
  }

  public String getOrderCancelUrl(String accountIdKey) {
    return baseUrl + "/v1/accounts/" + accountIdKey + "/orders/cancel";
  }
}
