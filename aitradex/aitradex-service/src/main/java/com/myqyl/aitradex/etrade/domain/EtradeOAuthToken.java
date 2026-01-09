package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "etrade_oauth_token")
@EntityListeners(AuditingEntityListener.class)
public class EtradeOAuthToken {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "account_id")
  private UUID accountId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
  private String accessTokenEncrypted;

  @Column(name = "access_token_secret_encrypted", columnDefinition = "TEXT")
  private String accessTokenSecretEncrypted;

  @Column(name = "token_type")
  private String tokenType = "Bearer";

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
  private String refreshTokenEncrypted;

  @Column(name = "request_token")
  private String requestToken;

  @Column(name = "request_token_secret")
  private String requestTokenSecret;

  @Column(name = "oauth_verifier")
  private String oauthVerifier;

  // Authorization attempt tracking fields
  @Column(name = "start_time")
  private OffsetDateTime startTime;

  @Column(name = "end_time")
  private OffsetDateTime endTime;

  @Column(name = "status")
  private String status; // PENDING, SUCCESS, FAILED

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "environment")
  private String environment; // SANDBOX, PRODUCTION

  @Column(name = "correlation_id")
  private String correlationId;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // Constructors
  public EtradeOAuthToken() {
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getAccessTokenEncrypted() {
    return accessTokenEncrypted;
  }

  public void setAccessTokenEncrypted(String accessTokenEncrypted) {
    this.accessTokenEncrypted = accessTokenEncrypted;
  }

  public String getAccessTokenSecretEncrypted() {
    return accessTokenSecretEncrypted;
  }

  public void setAccessTokenSecretEncrypted(String accessTokenSecretEncrypted) {
    this.accessTokenSecretEncrypted = accessTokenSecretEncrypted;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getRefreshTokenEncrypted() {
    return refreshTokenEncrypted;
  }

  public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
    this.refreshTokenEncrypted = refreshTokenEncrypted;
  }

  public String getRequestToken() {
    return requestToken;
  }

  public void setRequestToken(String requestToken) {
    this.requestToken = requestToken;
  }

  public String getRequestTokenSecret() {
    return requestTokenSecret;
  }

  public void setRequestTokenSecret(String requestTokenSecret) {
    this.requestTokenSecret = requestTokenSecret;
  }

  public String getOauthVerifier() {
    return oauthVerifier;
  }

  public void setOauthVerifier(String oauthVerifier) {
    this.oauthVerifier = oauthVerifier;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
}
