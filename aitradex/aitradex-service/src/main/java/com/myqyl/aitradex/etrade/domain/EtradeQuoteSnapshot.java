package com.myqyl.aitradex.etrade.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * E*TRADE Quote Snapshot entity.
 * 
 * Represents an append-only time-series snapshot of quote data.
 * Each call to Get Quotes creates a new row.
 */
@Entity
@Table(name = "etrade_quote_snapshot")
@EntityListeners(AuditingEntityListener.class)
public class EtradeQuoteSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "symbol", nullable = false)
  private String symbol;

  @Column(name = "quote_timestamp")
  private Long quoteTimestamp; // E*TRADE timestamp (epoch millis)

  @Column(name = "request_time", nullable = false)
  private OffsetDateTime requestTime;

  @Column(name = "detail_flag", length = 50)
  private String detailFlag; // ALL, FUNDAMENTAL, INTRADAY, OPTIONS, WEEK_52, MF_DETAIL

  // Quote fields
  @Column(name = "last_trade", precision = 19, scale = 8)
  private BigDecimal lastTrade;

  @Column(name = "previous_close", precision = 19, scale = 8)
  private BigDecimal previousClose;

  @Column(name = "open_price", precision = 19, scale = 8)
  private BigDecimal openPrice;

  @Column(name = "high", precision = 19, scale = 8)
  private BigDecimal high;

  @Column(name = "low", precision = 19, scale = 8)
  private BigDecimal low;

  @Column(name = "high52", precision = 19, scale = 8)
  private BigDecimal high52;

  @Column(name = "low52", precision = 19, scale = 8)
  private BigDecimal low52;

  @Column(name = "total_volume")
  private Long totalVolume;

  @Column(name = "volume")
  private Long volume;

  @Column(name = "change_close", precision = 19, scale = 8)
  private BigDecimal changeClose;

  @Column(name = "change_close_percentage", precision = 19, scale = 8)
  private BigDecimal changeClosePercentage;

  @Column(name = "bid", precision = 19, scale = 8)
  private BigDecimal bid;

  @Column(name = "ask", precision = 19, scale = 8)
  private BigDecimal ask;

  @Column(name = "bid_size")
  private Long bidSize;

  @Column(name = "ask_size")
  private Long askSize;

  @Column(name = "company_name", length = 255)
  private String companyName;

  @Column(name = "exchange", length = 50)
  private String exchange;

  @Column(name = "security_type", length = 50)
  private String securityType;

  @Column(name = "quote_type", length = 50)
  private String quoteType; // REALTIME, DELAYED

  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public EtradeQuoteSnapshot() {
    this.requestTime = OffsetDateTime.now();
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

  public Long getQuoteTimestamp() {
    return quoteTimestamp;
  }

  public void setQuoteTimestamp(Long quoteTimestamp) {
    this.quoteTimestamp = quoteTimestamp;
  }

  public OffsetDateTime getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(OffsetDateTime requestTime) {
    this.requestTime = requestTime;
  }

  public String getDetailFlag() {
    return detailFlag;
  }

  public void setDetailFlag(String detailFlag) {
    this.detailFlag = detailFlag;
  }

  public BigDecimal getLastTrade() {
    return lastTrade;
  }

  public void setLastTrade(BigDecimal lastTrade) {
    this.lastTrade = lastTrade;
  }

  public BigDecimal getPreviousClose() {
    return previousClose;
  }

  public void setPreviousClose(BigDecimal previousClose) {
    this.previousClose = previousClose;
  }

  public BigDecimal getOpenPrice() {
    return openPrice;
  }

  public void setOpenPrice(BigDecimal openPrice) {
    this.openPrice = openPrice;
  }

  public BigDecimal getHigh() {
    return high;
  }

  public void setHigh(BigDecimal high) {
    this.high = high;
  }

  public BigDecimal getLow() {
    return low;
  }

  public void setLow(BigDecimal low) {
    this.low = low;
  }

  public BigDecimal getHigh52() {
    return high52;
  }

  public void setHigh52(BigDecimal high52) {
    this.high52 = high52;
  }

  public BigDecimal getLow52() {
    return low52;
  }

  public void setLow52(BigDecimal low52) {
    this.low52 = low52;
  }

  public Long getTotalVolume() {
    return totalVolume;
  }

  public void setTotalVolume(Long totalVolume) {
    this.totalVolume = totalVolume;
  }

  public Long getVolume() {
    return volume;
  }

  public void setVolume(Long volume) {
    this.volume = volume;
  }

  public BigDecimal getChangeClose() {
    return changeClose;
  }

  public void setChangeClose(BigDecimal changeClose) {
    this.changeClose = changeClose;
  }

  public BigDecimal getChangeClosePercentage() {
    return changeClosePercentage;
  }

  public void setChangeClosePercentage(BigDecimal changeClosePercentage) {
    this.changeClosePercentage = changeClosePercentage;
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

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getSecurityType() {
    return securityType;
  }

  public void setSecurityType(String securityType) {
    this.securityType = securityType;
  }

  public String getQuoteType() {
    return quoteType;
  }

  public void setQuoteType(String quoteType) {
    this.quoteType = quoteType;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
