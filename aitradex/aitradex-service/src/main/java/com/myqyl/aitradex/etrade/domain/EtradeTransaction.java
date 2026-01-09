package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Transaction entity.
 * 
 * Represents a transaction that can be upserted by transactionId.
 * Transactions are updated when details are fetched.
 */
@Entity
@Table(name = "etrade_transaction", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"transaction_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class EtradeTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "transaction_id", nullable = false, unique = true)
  private String transactionId;

  @Column(name = "account_id_from_response")
  private String accountIdFromResponse;

  @Column(name = "transaction_date")
  private Long transactionDate; // Epoch milliseconds

  @Column(name = "amount", precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "transaction_type")
  private String transactionType;

  @Column(name = "inst_type")
  private String instType;

  @Column(name = "details_uri")
  private String detailsUri;

  // Transaction details (from Get Transaction Details)
  @Column(name = "category_id")
  private String categoryId;

  @Column(name = "category_parent_id")
  private String categoryParentId;

  @Column(name = "brokerage_transaction_type")
  private String brokerageTransactionType;

  // Raw response for reference (optional)
  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  @Column(name = "details_raw_response", columnDefinition = "jsonb")
  private String detailsRawResponse;

  @Column(name = "first_seen_at", nullable = false)
  private OffsetDateTime firstSeenAt;

  @Column(name = "last_updated_at", nullable = false)
  private OffsetDateTime lastUpdatedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // Constructors
  public EtradeTransaction() {
    this.firstSeenAt = OffsetDateTime.now();
    this.lastUpdatedAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID accountId) { this.accountId = accountId; }

  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

  public String getAccountIdFromResponse() { return accountIdFromResponse; }
  public void setAccountIdFromResponse(String accountIdFromResponse) { this.accountIdFromResponse = accountIdFromResponse; }

  public Long getTransactionDate() { return transactionDate; }
  public void setTransactionDate(Long transactionDate) { this.transactionDate = transactionDate; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getTransactionType() { return transactionType; }
  public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

  public String getInstType() { return instType; }
  public void setInstType(String instType) { this.instType = instType; }

  public String getDetailsUri() { return detailsUri; }
  public void setDetailsUri(String detailsUri) { this.detailsUri = detailsUri; }

  public String getCategoryId() { return categoryId; }
  public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

  public String getCategoryParentId() { return categoryParentId; }
  public void setCategoryParentId(String categoryParentId) { this.categoryParentId = categoryParentId; }

  public String getBrokerageTransactionType() { return brokerageTransactionType; }
  public void setBrokerageTransactionType(String brokerageTransactionType) { this.brokerageTransactionType = brokerageTransactionType; }

  public String getRawResponse() { return rawResponse; }
  public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

  public String getDetailsRawResponse() { return detailsRawResponse; }
  public void setDetailsRawResponse(String detailsRawResponse) { this.detailsRawResponse = detailsRawResponse; }

  public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
  public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

  public OffsetDateTime getLastUpdatedAt() { return lastUpdatedAt; }
  public void setLastUpdatedAt(OffsetDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
