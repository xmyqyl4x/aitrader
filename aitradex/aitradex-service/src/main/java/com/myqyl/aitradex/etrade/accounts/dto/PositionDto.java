package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Position DTO from Portfolio.
 * 
 * According to E*TRADE Accounts API documentation:
 * Represents a single position in an account portfolio.
 * Includes all fields from the Position model.
 */
public class PositionDto {

  private Long positionId; // int64
  private ProductDto product;
  private String symbolDescription;
  private Long dateAcquired; // int64 (epoch milliseconds)
  private Double pricePaid;
  private Double commissions;
  private Double otherFees;
  private Double quantity;
  private String positionIndicator; // TYPE2
  private String positionType; // LONG, SHORT
  private Double daysGain;
  private Double daysGainPct;
  private Double marketValue;
  private Double totalCost;
  private Double totalGain;
  private Double totalGainPct;
  private Double pctOfPortfolio;
  private Double costPerShare;
  private Double todayCommissions;
  private Double todayFees;
  private Double todayPricePaid;
  private Double todayQuantity;
  private Double adjPrevClose;
  private QuickViewDto quick;
  private String lotsDetails; // URL
  private String quoteDetails; // URL

  // Additional fields from documentation
  private String cusip;
  private String exchange;
  private Boolean isQuotable;
  private Double gainLoss;
  private Double gainLossPercent;
  private Double costBasis;
  private Double intrinsicValue;
  private Double timeValue;
  private Integer multiplier;
  private Integer digits;

  public PositionDto() {
  }

  // Getters and Setters

  public Long getPositionId() {
    return positionId;
  }

  public void setPositionId(Long positionId) {
    this.positionId = positionId;
  }

  public ProductDto getProduct() {
    return product;
  }

  public void setProduct(ProductDto product) {
    this.product = product;
  }

  public String getSymbolDescription() {
    return symbolDescription;
  }

  public void setSymbolDescription(String symbolDescription) {
    this.symbolDescription = symbolDescription;
  }

  public Long getDateAcquired() {
    return dateAcquired;
  }

  public void setDateAcquired(Long dateAcquired) {
    this.dateAcquired = dateAcquired;
  }

  public Double getPricePaid() {
    return pricePaid;
  }

  public void setPricePaid(Double pricePaid) {
    this.pricePaid = pricePaid;
  }

  public Double getCommissions() {
    return commissions;
  }

  public void setCommissions(Double commissions) {
    this.commissions = commissions;
  }

  public Double getOtherFees() {
    return otherFees;
  }

  public void setOtherFees(Double otherFees) {
    this.otherFees = otherFees;
  }

  public Double getQuantity() {
    return quantity;
  }

  public void setQuantity(Double quantity) {
    this.quantity = quantity;
  }

  public String getPositionIndicator() {
    return positionIndicator;
  }

  public void setPositionIndicator(String positionIndicator) {
    this.positionIndicator = positionIndicator;
  }

  public String getPositionType() {
    return positionType;
  }

  public void setPositionType(String positionType) {
    this.positionType = positionType;
  }

  public Double getDaysGain() {
    return daysGain;
  }

  public void setDaysGain(Double daysGain) {
    this.daysGain = daysGain;
  }

  public Double getDaysGainPct() {
    return daysGainPct;
  }

  public void setDaysGainPct(Double daysGainPct) {
    this.daysGainPct = daysGainPct;
  }

  public Double getMarketValue() {
    return marketValue;
  }

  public void setMarketValue(Double marketValue) {
    this.marketValue = marketValue;
  }

  public Double getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(Double totalCost) {
    this.totalCost = totalCost;
  }

  public Double getTotalGain() {
    return totalGain;
  }

  public void setTotalGain(Double totalGain) {
    this.totalGain = totalGain;
  }

  public Double getTotalGainPct() {
    return totalGainPct;
  }

  public void setTotalGainPct(Double totalGainPct) {
    this.totalGainPct = totalGainPct;
  }

  public Double getPctOfPortfolio() {
    return pctOfPortfolio;
  }

  public void setPctOfPortfolio(Double pctOfPortfolio) {
    this.pctOfPortfolio = pctOfPortfolio;
  }

  public Double getCostPerShare() {
    return costPerShare;
  }

  public void setCostPerShare(Double costPerShare) {
    this.costPerShare = costPerShare;
  }

  public Double getTodayCommissions() {
    return todayCommissions;
  }

  public void setTodayCommissions(Double todayCommissions) {
    this.todayCommissions = todayCommissions;
  }

  public Double getTodayFees() {
    return todayFees;
  }

  public void setTodayFees(Double todayFees) {
    this.todayFees = todayFees;
  }

  public Double getTodayPricePaid() {
    return todayPricePaid;
  }

  public void setTodayPricePaid(Double todayPricePaid) {
    this.todayPricePaid = todayPricePaid;
  }

  public Double getTodayQuantity() {
    return todayQuantity;
  }

  public void setTodayQuantity(Double todayQuantity) {
    this.todayQuantity = todayQuantity;
  }

  public Double getAdjPrevClose() {
    return adjPrevClose;
  }

  public void setAdjPrevClose(Double adjPrevClose) {
    this.adjPrevClose = adjPrevClose;
  }

  public QuickViewDto getQuick() {
    return quick;
  }

  public void setQuick(QuickViewDto quick) {
    this.quick = quick;
  }

  public String getLotsDetails() {
    return lotsDetails;
  }

  public void setLotsDetails(String lotsDetails) {
    this.lotsDetails = lotsDetails;
  }

  public String getQuoteDetails() {
    return quoteDetails;
  }

  public void setQuoteDetails(String quoteDetails) {
    this.quoteDetails = quoteDetails;
  }

  public String getCusip() {
    return cusip;
  }

  public void setCusip(String cusip) {
    this.cusip = cusip;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public Boolean getIsQuotable() {
    return isQuotable;
  }

  public void setIsQuotable(Boolean isQuotable) {
    this.isQuotable = isQuotable;
  }

  public Double getGainLoss() {
    return gainLoss;
  }

  public void setGainLoss(Double gainLoss) {
    this.gainLoss = gainLoss;
  }

  public Double getGainLossPercent() {
    return gainLossPercent;
  }

  public void setGainLossPercent(Double gainLossPercent) {
    this.gainLossPercent = gainLossPercent;
  }

  public Double getCostBasis() {
    return costBasis;
  }

  public void setCostBasis(Double costBasis) {
    this.costBasis = costBasis;
  }

  public Double getIntrinsicValue() {
    return intrinsicValue;
  }

  public void setIntrinsicValue(Double intrinsicValue) {
    this.intrinsicValue = intrinsicValue;
  }

  public Double getTimeValue() {
    return timeValue;
  }

  public void setTimeValue(Double timeValue) {
    this.timeValue = timeValue;
  }

  public Integer getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(Integer multiplier) {
    this.multiplier = multiplier;
  }

  public Integer getDigits() {
    return digits;
  }

  public void setDigits(Integer digits) {
    this.digits = digits;
  }
}
