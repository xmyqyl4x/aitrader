package com.myqyl.aitradex.etrade.market.dto;

/**
 * Option DTO for Option Chains.
 * 
 * According to E*TRADE Market API documentation:
 * Represents a single option (call or put) in an option chain.
 */
public class OptionDto {

  private String optionCategory; // STANDARD, MINI, etc.
  private String optionRootSymbol;
  private Long timestamp;
  private Boolean adjustedFlag;
  private String displaySymbol;
  private String optionType; // CALL, PUT
  private Double strikePrice;
  private String symbol;
  private Double bid;
  private Double ask;
  private Long bidSize;
  private Long askSize;
  private String inTheMoney; // y, n
  private Long volume;
  private Long openInterest;
  private Double netChange;
  private Double lastPrice;
  private String quoteDetail; // URL to quote detail
  private String osiKey; // OCC option symbol
  private OptionGreeksDto optionGreeks;

  public OptionDto() {
  }

  // Getters and Setters
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

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public Boolean getAdjustedFlag() {
    return adjustedFlag;
  }

  public void setAdjustedFlag(Boolean adjustedFlag) {
    this.adjustedFlag = adjustedFlag;
  }

  public String getDisplaySymbol() {
    return displaySymbol;
  }

  public void setDisplaySymbol(String displaySymbol) {
    this.displaySymbol = displaySymbol;
  }

  public String getOptionType() {
    return optionType;
  }

  public void setOptionType(String optionType) {
    this.optionType = optionType;
  }

  public Double getStrikePrice() {
    return strikePrice;
  }

  public void setStrikePrice(Double strikePrice) {
    this.strikePrice = strikePrice;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Double getBid() {
    return bid;
  }

  public void setBid(Double bid) {
    this.bid = bid;
  }

  public Double getAsk() {
    return ask;
  }

  public void setAsk(Double ask) {
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

  public String getInTheMoney() {
    return inTheMoney;
  }

  public void setInTheMoney(String inTheMoney) {
    this.inTheMoney = inTheMoney;
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

  public Double getNetChange() {
    return netChange;
  }

  public void setNetChange(Double netChange) {
    this.netChange = netChange;
  }

  public Double getLastPrice() {
    return lastPrice;
  }

  public void setLastPrice(Double lastPrice) {
    this.lastPrice = lastPrice;
  }

  public String getQuoteDetail() {
    return quoteDetail;
  }

  public void setQuoteDetail(String quoteDetail) {
    this.quoteDetail = quoteDetail;
  }

  public String getOsiKey() {
    return osiKey;
  }

  public void setOsiKey(String osiKey) {
    this.osiKey = osiKey;
  }

  public OptionGreeksDto getOptionGreeks() {
    return optionGreeks;
  }

  public void setOptionGreeks(OptionGreeksDto optionGreeks) {
    this.optionGreeks = optionGreeks;
  }
}
