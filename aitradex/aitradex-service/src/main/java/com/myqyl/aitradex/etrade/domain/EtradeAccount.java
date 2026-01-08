package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "etrade_account")
@EntityListeners(AuditingEntityListener.class)
public class EtradeAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "account_id_key", nullable = false, unique = true)
  private String accountIdKey;

  @Column(name = "account_type")
  private String accountType;

  @Column(name = "account_name")
  private String accountName;

  @Column(name = "account_status")
  private String accountStatus;

  @Column(name = "linked_at", nullable = false)
  private OffsetDateTime linkedAt;

  @Column(name = "last_synced_at")
  private OffsetDateTime lastSyncedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // Constructors
  public EtradeAccount() {
    this.linkedAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getAccountIdKey() {
    return accountIdKey;
  }

  public void setAccountIdKey(String accountIdKey) {
    this.accountIdKey = accountIdKey;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }

  public OffsetDateTime getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(OffsetDateTime linkedAt) {
    this.linkedAt = linkedAt;
  }

  public OffsetDateTime getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
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
}
