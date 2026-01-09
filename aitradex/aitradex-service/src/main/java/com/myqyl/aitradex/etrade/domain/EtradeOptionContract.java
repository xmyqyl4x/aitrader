package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Option Contract entity.
 * 
 * Represents an option contract found in option chains.
 * Upserted by optionSymbol (unique).
 */
@Entity
@Table(name = "etrade_option_contract")
@EntityListeners(AuditingEntityListener.class)
public class EtradeOptionContract {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "option_symbol", nullable = false, unique = true, length = 100)
  private String optionSymbol;

  @Column(name = "osi_key", length = 100)
  private String osiKey; // Option Symbol Identifier

  @Column(name = "underlying_symbol", nullable = false)
  private String underlyingSymbol;

  @Column(name = "option_type", length = 10)
  private String optionType; // CALL, PUT

  @Column(name = "strike_price", precision = 19, scale = 8)
  private BigDecimal strikePrice;

  @Column(name = "expiry_year")
  private Integer expiryYear;

  @Column(name = "expiry_month")
  private Integer expiryMonth;

  @Column(name = "expiry_day")
  private Integer expiryDay;

  @Column(name = "option_category", length = 50)
  private String optionCategory; // STANDARD, ALL, MINI

  @Column(name = "option_root_symbol", length = 50)
  private String optionRootSymbol;

  @Column(name = "display_symbol", length = 100)
  private String displaySymbol;

  @Column(name = "adjusted_flag")
  private Boolean adjustedFlag;

  // Quote fields
  @Column(name = "bid", precision = 19, scale = 8)
  private BigDecimal bid;

  @Column(name = "ask", precision = 19, scale = 8)
  private BigDecimal ask;

  @Column(name = "bid_size")
  private Long bidSize;

  @Column(name = "ask_size")
  private Long askSize;

  @Column(name = "last_price", precision = 19, scale = 8)
  private BigDecimal lastPrice;

  @Column(name = "volume")
  private Long volume;

  @Column(name = "open_interest")
  private Long openInterest;

  @Column(name = "net_change", precision = 19, scale = 8)
  private BigDecimal netChange;

  @Column(name = "in_the_money", length = 10)
  private String inTheMoney;

  @Column(name = "quote_detail", columnDefinition = "TEXT")
  private String quoteDetail; // URL to quote endpoint

  // Option Greeks
  @Column(name = "delta", precision = 19, scale = 8)
  private BigDecimal delta;

  @Column(name = "gamma", precision = 19, scale = 8)
  private BigDecimal gamma;

  @Column(name = "theta", precision = 19, scale = 8)
  private BigDecimal theta;

  @Column(name = "vega", precision = 19, scale = 8)
  private BigDecimal vega;

  @Column(name = "rho", precision = 19, scale = 8)
  private BigDecimal rho;

  @Column(name = "iv", precision = 19, scale = 8)
  private BigDecimal iv; // Implied Volatility

  @Column(name = "greeks_current_value")
  private Boolean greeksCurrentValue;

  @Column(name = "last_synced_at", nullable = false)
  private OffsetDateTime lastSyncedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public EtradeOptionContract() {
    this.lastSyncedAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getOptionSymbol() {
    return optionSymbol;
  }

  public void setOptionSymbol(String optionSymbol) {
    this.optionSymbol = optionSymbol;
  }

  public String getOsiKey() {
    return osiKey;
  }

  public void setOsiKey(String osiKey) {
    this.osiKey = osiKey;
  }

  public String getUnderlyingSymbol() {
    return underlyingSymbol;
  }

  public void setUnderlyingSymbol(String underlyingSymbol) {
    this.underlyingSymbol = underlyingSymbol;
  }

  public String getOptionType() {
    return optionType;
  }

  public void setOptionType(String optionType) {
    this.optionType = optionType;
  }

  public BigDecimal getStrikePrice() {
    return strikePrice;
  }

  public void setStrikePrice(BigDecimal strikePrice) {
    this.strikePrice = strikePrice;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryDay() {
    return expiryDay;
  }

  public void setExpiryDay(Integer expiryDay) {
    this.expiryDay = expiryDay;
  }

  public String getOptionCategory() {
    return optionCategory;
  }

  public void setOptionCategory(String optionCategory) {
    this.optionCategory = optionCategory;
  }

  public String getOptionRootSymbol() {
    return optionRootSymbol;
  }

  public void setOptionRootSymbol(String optionRootSymbol) {
    this.optionRootSymbol = optionRootSymbol;
  }

  public String getDisplaySymbol() {
    return displaySymbol;
  }

  public void setDisplaySymbol(String displaySymbol) {
    this.displaySymbol = displaySymbol;
  }

  public Boolean getAdjustedFlag() {
    return adjustedFlag;
  }

  public void setAdjustedFlag(Boolean adjustedFlag) {
    this.adjustedFlag = adjustedFlag;
  }

  public BigDecimal getBid() {
    return bid;
  }

  public void setBid(BigDecimal bid) {
    this.bid = bid;
  }

  public BigDecimal getAsk() {
    return ask;
  }

  public void setAsk(BigDecimal ask) {
    this.ask = ask;
  }

  public Long getBidSize() {
    return bidSize;
  }

  public void setBidSize(Long bidSize) {
    this.bidSize = bidSize;
  }

  public Long getAskSize() {
    return askSize;
  }

  public void setAskSize(Long askSize) {
    this.askSize = askSize;
  }

  public BigDecimal getLastPrice() {
    return lastPrice;
  }

  public void setLastPrice(BigDecimal lastPrice) {
    this.lastPrice = lastPrice;
  }

  public Long getVolume() {
    return volume;
  }

  public void setVolume(Long volume) {
    this.volume = volume;
  }

  public Long getOpenInterest() {
    return openInterest;
  }

  public void setOpenInterest(Long openInterest) {
    this.openInterest = openInterest;
  }

  public BigDecimal getNetChange() {
    return netChange;
  }

  public void setNetChange(BigDecimal netChange) {
    this.netChange = netChange;
  }

  public String getInTheMoney() {
    return inTheMoney;
  }

  public void setInTheMoney(String inTheMoney) {
    this.inTheMoney = inTheMoney;
  }

  public String getQuoteDetail() {
    return quoteDetail;
  }

  public void setQuoteDetail(String quoteDetail) {
    this.quoteDetail = quoteDetail;
  }

  public BigDecimal getDelta() {
    return delta;
  }

  public void setDelta(BigDecimal delta) {
    this.delta = delta;
  }

  public BigDecimal getGamma() {
    return gamma;
  }

  public void setGamma(BigDecimal gamma) {
    this.gamma = gamma;
  }

  public BigDecimal getTheta() {
    return theta;
  }

  public void setTheta(BigDecimal theta) {
    this.theta = theta;
  }

  public BigDecimal getVega() {
    return vega;
  }

  public void setVega(BigDecimal vega) {
    this.vega = vega;
  }

  public BigDecimal getRho() {
    return rho;
  }

  public void setRho(BigDecimal rho) {
    this.rho = rho;
  }

  public BigDecimal getIv() {
    return iv;
  }

  public void setIv(BigDecimal iv) {
    this.iv = iv;
  }

  public Boolean getGreeksCurrentValue() {
    return greeksCurrentValue;
  }

  public void setGreeksCurrentValue(Boolean greeksCurrentValue) {
    this.greeksCurrentValue = greeksCurrentValue;
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
