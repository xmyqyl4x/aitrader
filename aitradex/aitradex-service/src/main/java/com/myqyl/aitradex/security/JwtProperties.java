package com.myqyl.aitradex.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

  /**
   * Symmetric signing key for HS256/HS512 tokens. Should be at least 32 bytes for HS256.
   */
  private String secret;

  /** Expected token issuer. */
  private String issuer;

  /** Optional audience to require on incoming tokens. */
  private String audience;

  /** Allowed clock skew for validating exp/nbf in seconds. */
  private long clockSkewSeconds = 60;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAudience() {
    return audience;
  }

  public void setAudience(String audience) {
    this.audience = audience;
  }

  public long getClockSkewSeconds() {
    return clockSkewSeconds;
  }

  public void setClockSkewSeconds(long clockSkewSeconds) {
    this.clockSkewSeconds = clockSkewSeconds;
  }

  public boolean hasAudience() {
    return StringUtils.hasText(audience);
  }
}
