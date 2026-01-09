package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Balance Snapshot entity.
 * 
 * Represents an append-only history of account balance snapshots.
 * Each call to Get Account Balance creates a new row.
 */
@Entity
@Table(name = "etrade_balance")
@EntityListeners(AuditingEntityListener.class)
public class EtradeBalance {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "snapshot_time", nullable = false)
  private OffsetDateTime snapshotTime;

  // Cash section
  @Column(name = "cash_balance", precision = 19, scale = 4)
  private BigDecimal cashBalance;

  @Column(name = "cash_available", precision = 19, scale = 4)
  private BigDecimal cashAvailable;

  @Column(name = "uncleared_deposits", precision = 19, scale = 4)
  private BigDecimal unclearedDeposits;

  @Column(name = "cash_sweep", precision = 19, scale = 4)
  private BigDecimal cashSweep;

  // Margin section
  @Column(name = "margin_balance", precision = 19, scale = 4)
  private BigDecimal marginBalance;

  @Column(name = "margin_available", precision = 19, scale = 4)
  private BigDecimal marginAvailable;

  @Column(name = "margin_buying_power", precision = 19, scale = 4)
  private BigDecimal marginBuyingPower;

  @Column(name = "day_trading_buying_power", precision = 19, scale = 4)
  private BigDecimal dayTradingBuyingPower;

  // Computed section
  @Column(name = "total_value", precision = 19, scale = 4)
  private BigDecimal totalValue;

  @Column(name = "net_value", precision = 19, scale = 4)
  private BigDecimal netValue;

  @Column(name = "settled_cash", precision = 19, scale = 4)
  private BigDecimal settledCash;

  @Column(name = "open_calls", precision = 19, scale = 4)
  private BigDecimal openCalls;

  @Column(name = "open_puts", precision = 19, scale = 4)
  private BigDecimal openPuts;

  // Account metadata
  @Column(name = "account_id_from_response")
  private String accountIdFromResponse;

  @Column(name = "account_type")
  private String accountType;

  @Column(name = "account_description")
  private String accountDescription;

  @Column(name = "account_mode")
  private String accountMode;

  // Raw response for reference (optional)
  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  // Constructors
  public EtradeBalance() {
    this.snapshotTime = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID accountId) { this.accountId = accountId; }

  public OffsetDateTime getSnapshotTime() { return snapshotTime; }
  public void setSnapshotTime(OffsetDateTime snapshotTime) { this.snapshotTime = snapshotTime; }

  public BigDecimal getCashBalance() { return cashBalance; }
  public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

  public BigDecimal getCashAvailable() { return cashAvailable; }
  public void setCashAvailable(BigDecimal cashAvailable) { this.cashAvailable = cashAvailable; }

  public BigDecimal getUnclearedDeposits() { return unclearedDeposits; }
  public void setUnclearedDeposits(BigDecimal unclearedDeposits) { this.unclearedDeposits = unclearedDeposits; }

  public BigDecimal getCashSweep() { return cashSweep; }
  public void setCashSweep(BigDecimal cashSweep) { this.cashSweep = cashSweep; }

  public BigDecimal getMarginBalance() { return marginBalance; }
  public void setMarginBalance(BigDecimal marginBalance) { this.marginBalance = marginBalance; }

  public BigDecimal getMarginAvailable() { return marginAvailable; }
  public void setMarginAvailable(BigDecimal marginAvailable) { this.marginAvailable = marginAvailable; }

  public BigDecimal getMarginBuyingPower() { return marginBuyingPower; }
  public void setMarginBuyingPower(BigDecimal marginBuyingPower) { this.marginBuyingPower = marginBuyingPower; }

  public BigDecimal getDayTradingBuyingPower() { return dayTradingBuyingPower; }
  public void setDayTradingBuyingPower(BigDecimal dayTradingBuyingPower) { this.dayTradingBuyingPower = dayTradingBuyingPower; }

  public BigDecimal getTotalValue() { return totalValue; }
  public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

  public BigDecimal getNetValue() { return netValue; }
  public void setNetValue(BigDecimal netValue) { this.netValue = netValue; }

  public BigDecimal getSettledCash() { return settledCash; }
  public void setSettledCash(BigDecimal settledCash) { this.settledCash = settledCash; }

  public BigDecimal getOpenCalls() { return openCalls; }
  public void setOpenCalls(BigDecimal openCalls) { this.openCalls = openCalls; }

  public BigDecimal getOpenPuts() { return openPuts; }
  public void setOpenPuts(BigDecimal openPuts) { this.openPuts = openPuts; }

  public String getAccountIdFromResponse() { return accountIdFromResponse; }
  public void setAccountIdFromResponse(String accountIdFromResponse) { this.accountIdFromResponse = accountIdFromResponse; }

  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }

  public String getAccountDescription() { return accountDescription; }
  public void setAccountDescription(String accountDescription) { this.accountDescription = accountDescription; }

  public String getAccountMode() { return accountMode; }
  public void setAccountMode(String accountMode) { this.accountMode = accountMode; }

  public String getRawResponse() { return rawResponse; }
  public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
