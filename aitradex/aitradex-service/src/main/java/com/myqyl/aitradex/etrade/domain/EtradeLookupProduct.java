package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Lookup Product entity.
 * 
 * Represents a product found via Lookup Product API.
 * Upserted by (symbol, productType) combination.
 */
@Entity
@Table(name = "etrade_lookup_product", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "product_type"})
})
@EntityListeners(AuditingEntityListener.class)
public class EtradeLookupProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "symbol", nullable = false)
  private String symbol;

  @Column(name = "product_type")
  private String productType;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "last_seen_at", nullable = false)
  private OffsetDateTime lastSeenAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public EtradeLookupProduct() {
    this.lastSeenAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getProductType() {
    return productType;
  }

  public void setProductType(String productType) {
    this.productType = productType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OffsetDateTime getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(OffsetDateTime lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
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
