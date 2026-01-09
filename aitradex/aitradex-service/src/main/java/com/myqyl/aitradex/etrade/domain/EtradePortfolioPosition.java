package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Portfolio Position entity.
 * 
 * Represents a position in an account portfolio that can be upserted by positionId.
 * Positions are updated when portfolio view is called.
 */
@Entity
@Table(name = "etrade_portfolio_position", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "position_id"}, name = "uk_etrade_portfolio_position_account_position")
})
@EntityListeners(AuditingEntityListener.class)
public class EtradePortfolioPosition {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "position_id", nullable = false)
  private Long positionId;

  // Product information
  @Column(name = "symbol")
  private String symbol;

  @Column(name = "symbol_description")
  private String symbolDescription;

  @Column(name = "security_type")
  private String securityType;

  @Column(name = "cusip")
  private String cusip;

  @Column(name = "exchange")
  private String exchange;

  @Column(name = "is_quotable")
  private Boolean isQuotable;

  // Position details
  @Column(name = "date_acquired")
  private Long dateAcquired; // Epoch milliseconds

  @Column(name = "price_paid", precision = 19, scale = 8)
  private BigDecimal pricePaid;

  @Column(name = "commissions", precision = 19, scale = 8)
  private BigDecimal commissions;

  @Column(name = "other_fees", precision = 19, scale = 8)
  private BigDecimal otherFees;

  @Column(name = "quantity", precision = 19, scale = 8)
  private BigDecimal quantity;

  @Column(name = "position_indicator")
  private String positionIndicator;

  @Column(name = "position_type")
  private String positionType; // LONG, SHORT

  // Market values
  @Column(name = "days_gain", precision = 19, scale = 8)
  private BigDecimal daysGain;

  @Column(name = "days_gain_pct", precision = 19, scale = 8)
  private BigDecimal daysGainPct;

  @Column(name = "market_value", precision = 19, scale = 8)
  private BigDecimal marketValue;

  @Column(name = "total_cost", precision = 19, scale = 8)
  private BigDecimal totalCost;

  @Column(name = "total_gain", precision = 19, scale = 8)
  private BigDecimal totalGain;

  @Column(name = "total_gain_pct", precision = 19, scale = 8)
  private BigDecimal totalGainPct;

  @Column(name = "pct_of_portfolio", precision = 19, scale = 8)
  private BigDecimal pctOfPortfolio;

  @Column(name = "cost_per_share", precision = 19, scale = 8)
  private BigDecimal costPerShare;

  @Column(name = "gain_loss", precision = 19, scale = 8)
  private BigDecimal gainLoss;

  @Column(name = "gain_loss_percent", precision = 19, scale = 8)
  private BigDecimal gainLossPercent;

  @Column(name = "cost_basis", precision = 19, scale = 8)
  private BigDecimal costBasis;

  // Option-specific fields
  @Column(name = "intrinsic_value", precision = 19, scale = 8)
  private BigDecimal intrinsicValue;

  @Column(name = "time_value", precision = 19, scale = 8)
  private BigDecimal timeValue;

  @Column(name = "multiplier")
  private Integer multiplier;

  @Column(name = "digits")
  private Integer digits;

  // URLs
  @Column(name = "lots_details_uri")
  private String lotsDetailsUri;

  @Column(name = "quote_details_uri")
  private String quoteDetailsUri;

  // Raw response for reference (optional)
  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  @Column(name = "snapshot_time", nullable = false)
  private OffsetDateTime snapshotTime;

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
  public EtradePortfolioPosition() {
    this.snapshotTime = OffsetDateTime.now();
    this.firstSeenAt = OffsetDateTime.now();
    this.lastUpdatedAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID accountId) { this.accountId = accountId; }

  public Long getPositionId() { return positionId; }
  public void setPositionId(Long positionId) { this.positionId = positionId; }

  public String getSymbol() { return symbol; }
  public void setSymbol(String symbol) { this.symbol = symbol; }

  public String getSymbolDescription() { return symbolDescription; }
  public void setSymbolDescription(String symbolDescription) { this.symbolDescription = symbolDescription; }

  public String getSecurityType() { return securityType; }
  public void setSecurityType(String securityType) { this.securityType = securityType; }

  public String getCusip() { return cusip; }
  public void setCusip(String cusip) { this.cusip = cusip; }

  public String getExchange() { return exchange; }
  public void setExchange(String exchange) { this.exchange = exchange; }

  public Boolean getIsQuotable() { return isQuotable; }
  public void setIsQuotable(Boolean isQuotable) { this.isQuotable = isQuotable; }

  public Long getDateAcquired() { return dateAcquired; }
  public void setDateAcquired(Long dateAcquired) { this.dateAcquired = dateAcquired; }

  public BigDecimal getPricePaid() { return pricePaid; }
  public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }

  public BigDecimal getCommissions() { return commissions; }
  public void setCommissions(BigDecimal commissions) { this.commissions = commissions; }

  public BigDecimal getOtherFees() { return otherFees; }
  public void setOtherFees(BigDecimal otherFees) { this.otherFees = otherFees; }

  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

  public String getPositionIndicator() { return positionIndicator; }
  public void setPositionIndicator(String positionIndicator) { this.positionIndicator = positionIndicator; }

  public String getPositionType() { return positionType; }
  public void setPositionType(String positionType) { this.positionType = positionType; }

  public BigDecimal getDaysGain() { return daysGain; }
  public void setDaysGain(BigDecimal daysGain) { this.daysGain = daysGain; }

  public BigDecimal getDaysGainPct() { return daysGainPct; }
  public void setDaysGainPct(BigDecimal daysGainPct) { this.daysGainPct = daysGainPct; }

  public BigDecimal getMarketValue() { return marketValue; }
  public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

  public BigDecimal getTotalCost() { return totalCost; }
  public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

  public BigDecimal getTotalGain() { return totalGain; }
  public void setTotalGain(BigDecimal totalGain) { this.totalGain = totalGain; }

  public BigDecimal getTotalGainPct() { return totalGainPct; }
  public void setTotalGainPct(BigDecimal totalGainPct) { this.totalGainPct = totalGainPct; }

  public BigDecimal getPctOfPortfolio() { return pctOfPortfolio; }
  public void setPctOfPortfolio(BigDecimal pctOfPortfolio) { this.pctOfPortfolio = pctOfPortfolio; }

  public BigDecimal getCostPerShare() { return costPerShare; }
  public void setCostPerShare(BigDecimal costPerShare) { this.costPerShare = costPerShare; }

  public BigDecimal getGainLoss() { return gainLoss; }
  public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }

  public BigDecimal getGainLossPercent() { return gainLossPercent; }
  public void setGainLossPercent(BigDecimal gainLossPercent) { this.gainLossPercent = gainLossPercent; }

  public BigDecimal getCostBasis() { return costBasis; }
  public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }

  public BigDecimal getIntrinsicValue() { return intrinsicValue; }
  public void setIntrinsicValue(BigDecimal intrinsicValue) { this.intrinsicValue = intrinsicValue; }

  public BigDecimal getTimeValue() { return timeValue; }
  public void setTimeValue(BigDecimal timeValue) { this.timeValue = timeValue; }

  public Integer getMultiplier() { return multiplier; }
  public void setMultiplier(Integer multiplier) { this.multiplier = multiplier; }

  public Integer getDigits() { return digits; }
  public void setDigits(Integer digits) { this.digits = digits; }

  public String getLotsDetailsUri() { return lotsDetailsUri; }
  public void setLotsDetailsUri(String lotsDetailsUri) { this.lotsDetailsUri = lotsDetailsUri; }

  public String getQuoteDetailsUri() { return quoteDetailsUri; }
  public void setQuoteDetailsUri(String quoteDetailsUri) { this.quoteDetailsUri = quoteDetailsUri; }

  public String getRawResponse() { return rawResponse; }
  public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

  public OffsetDateTime getSnapshotTime() { return snapshotTime; }
  public void setSnapshotTime(OffsetDateTime snapshotTime) { this.snapshotTime = snapshotTime; }

  public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
  public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

  public OffsetDateTime getLastUpdatedAt() { return lastUpdatedAt; }
  public void setLastUpdatedAt(OffsetDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
