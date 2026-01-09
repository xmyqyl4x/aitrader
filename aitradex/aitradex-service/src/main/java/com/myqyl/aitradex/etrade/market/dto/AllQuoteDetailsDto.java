package com.myqyl.aitradex.etrade.market.dto;

/**
 * All Quote Details DTO for Quote.
 * 
 * According to E*TRADE Market API documentation:
 * Contains comprehensive quote details for stocks, options, and mutual funds.
 * Includes all fields from the ALL detailFlag response.
 */
public class AllQuoteDetailsDto {

  // Product information
  private QuoteProductDto product;
  
  // Price information
  private Double lastTrade;
  private Double previousClose;
  private Double open;
  private Double high;
  private Double low;
  private Double high52;
  private Double low52;
  
  // Volume information
  private Long totalVolume;
  private Long volume;
  
  // Change information
  private Double changeClose;
  private Double changeClosePercentage;
  private String dirLast; // Direction of movement
  
  // Bid/Ask information
  private Double bid;
  private Double ask;
  private Long bidSize;
  private Long askSize;
  private String bidExchange;
  private String bidTime;
  private String askTime;
  
  // Company information
  private String companyName;
  
  // Dividend information
  private Double dividend;
  private Long exDividendDate;
  private Long nextEarningDate;
  
  // Earnings information
  private Double eps; // Earnings per share
  private Double estEarnings; // Projected Earnings per share
  
  // Option-specific fields
  private Boolean adjustedFlag;
  private Integer daysToExpiration;
  private String optionStyle; // European or American
  private String optionUnderlier;
  private String optionUnderlierExchange;
  private Long openInterest;
  
  // Option Greeks (if available)
  private OptionGreeksDto optionGreeks;
  
  // Additional fields
  private Long dateTime; // Epoch time
  private String quoteType; // DELAYED, REALTIME
  private SelectedEDDto selectedED;
  
  // MutualFund-specific fields
  private Double netAssetValue;
  private Double publicOfferPrice;

  public AllQuoteDetailsDto() {
  }

  // Getters and Setters
  public QuoteProductDto getProduct() {
    return product;
  }

  public void setProduct(QuoteProductDto product) {
    this.product = product;
  }

  public Double getLastTrade() {
    return lastTrade;
  }

  public void setLastTrade(Double lastTrade) {
    this.lastTrade = lastTrade;
  }

  public Double getPreviousClose() {
    return previousClose;
  }

  public void setPreviousClose(Double previousClose) {
    this.previousClose = previousClose;
  }

  public Double getOpen() {
    return open;
  }

  public void setOpen(Double open) {
    this.open = open;
  }

  public Double getHigh() {
    return high;
  }

  public void setHigh(Double high) {
    this.high = high;
  }

  public Double getLow() {
    return low;
  }

  public void setLow(Double low) {
    this.low = low;
  }

  public Double getHigh52() {
    return high52;
  }

  public void setHigh52(Double high52) {
    this.high52 = high52;
  }

  public Double getLow52() {
    return low52;
  }

  public void setLow52(Double low52) {
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

  public Double getChangeClose() {
    return changeClose;
  }

  public void setChangeClose(Double changeClose) {
    this.changeClose = changeClose;
  }

  public Double getChangeClosePercentage() {
    return changeClosePercentage;
  }

  public void setChangeClosePercentage(Double changeClosePercentage) {
    this.changeClosePercentage = changeClosePercentage;
  }

  public String getDirLast() {
    return dirLast;
  }

  public void setDirLast(String dirLast) {
    this.dirLast = dirLast;
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

  public String getBidExchange() {
    return bidExchange;
  }

  public void setBidExchange(String bidExchange) {
    this.bidExchange = bidExchange;
  }

  public String getBidTime() {
    return bidTime;
  }

  public void setBidTime(String bidTime) {
    this.bidTime = bidTime;
  }

  public String getAskTime() {
    return askTime;
  }

  public void setAskTime(String askTime) {
    this.askTime = askTime;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public Double getDividend() {
    return dividend;
  }

  public void setDividend(Double dividend) {
    this.dividend = dividend;
  }

  public Long getExDividendDate() {
    return exDividendDate;
  }

  public void setExDividendDate(Long exDividendDate) {
    this.exDividendDate = exDividendDate;
  }

  public Long getNextEarningDate() {
    return nextEarningDate;
  }

  public void setNextEarningDate(Long nextEarningDate) {
    this.nextEarningDate = nextEarningDate;
  }

  public Double getEps() {
    return eps;
  }

  public void setEps(Double eps) {
    this.eps = eps;
  }

  public Double getEstEarnings() {
    return estEarnings;
  }

  public void setEstEarnings(Double estEarnings) {
    this.estEarnings = estEarnings;
  }

  public Boolean getAdjustedFlag() {
    return adjustedFlag;
  }

  public void setAdjustedFlag(Boolean adjustedFlag) {
    this.adjustedFlag = adjustedFlag;
  }

  public Integer getDaysToExpiration() {
    return daysToExpiration;
  }

  public void setDaysToExpiration(Integer daysToExpiration) {
    this.daysToExpiration = daysToExpiration;
  }

  public String getOptionStyle() {
    return optionStyle;
  }

  public void setOptionStyle(String optionStyle) {
    this.optionStyle = optionStyle;
  }

  public String getOptionUnderlier() {
    return optionUnderlier;
  }

  public void setOptionUnderlier(String optionUnderlier) {
    this.optionUnderlier = optionUnderlier;
  }

  public String getOptionUnderlierExchange() {
    return optionUnderlierExchange;
  }

  public void setOptionUnderlierExchange(String optionUnderlierExchange) {
    this.optionUnderlierExchange = optionUnderlierExchange;
  }

  public Long getOpenInterest() {
    return openInterest;
  }

  public void setOpenInterest(Long openInterest) {
    this.openInterest = openInterest;
  }

  public OptionGreeksDto getOptionGreeks() {
    return optionGreeks;
  }

  public void setOptionGreeks(OptionGreeksDto optionGreeks) {
    this.optionGreeks = optionGreeks;
  }

  public Long getDateTime() {
    return dateTime;
  }

  public void setDateTime(Long dateTime) {
    this.dateTime = dateTime;
  }

  public String getQuoteType() {
    return quoteType;
  }

  public void setQuoteType(String quoteType) {
    this.quoteType = quoteType;
  }

  public SelectedEDDto getSelectedED() {
    return selectedED;
  }

  public void setSelectedED(SelectedEDDto selectedED) {
    this.selectedED = selectedED;
  }

  public Double getNetAssetValue() {
    return netAssetValue;
  }

  public void setNetAssetValue(Double netAssetValue) {
    this.netAssetValue = netAssetValue;
  }

  public Double getPublicOfferPrice() {
    return publicOfferPrice;
  }

  public void setPublicOfferPrice(Double publicOfferPrice) {
    this.publicOfferPrice = publicOfferPrice;
  }
}
