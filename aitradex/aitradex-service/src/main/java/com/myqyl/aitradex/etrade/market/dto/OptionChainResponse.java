package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Get Option Chains API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains option chain data with option pairs and metadata.
 */
public class OptionChainResponse {

  private String symbol;
  private Double nearPrice;
  private Boolean adjustedFlag;
  private String optionChainType;
  private Long timestamp;
  private String quoteType; // DELAYED, REALTIME
  
  @Valid
  private List<OptionPairDto> optionPairs;
  
  private SelectedEDDto selectedED;

  public OptionChainResponse() {
    this.optionPairs = new ArrayList<>();
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Double getNearPrice() {
    return nearPrice;
  }

  public void setNearPrice(Double nearPrice) {
    this.nearPrice = nearPrice;
  }

  public Boolean getAdjustedFlag() {
    return adjustedFlag;
  }

  public void setAdjustedFlag(Boolean adjustedFlag) {
    this.adjustedFlag = adjustedFlag;
  }

  public String getOptionChainType() {
    return optionChainType;
  }

  public void setOptionChainType(String optionChainType) {
    this.optionChainType = optionChainType;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public String getQuoteType() {
    return quoteType;
  }

  public void setQuoteType(String quoteType) {
    this.quoteType = quoteType;
  }

  public List<OptionPairDto> getOptionPairs() {
    return optionPairs;
  }

  public void setOptionPairs(List<OptionPairDto> optionPairs) {
    this.optionPairs = optionPairs != null ? optionPairs : new ArrayList<>();
  }

  public SelectedEDDto getSelectedED() {
    return selectedED;
  }

  public void setSelectedED(SelectedEDDto selectedED) {
    this.selectedED = selectedED;
  }
}
