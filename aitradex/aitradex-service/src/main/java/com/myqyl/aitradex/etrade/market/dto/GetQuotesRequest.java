package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Get Quotes API.
 * 
 * According to E*TRADE Market API documentation:
 * Query parameters:
 * - symbols (path, required): One or more (comma-separated) symbols, up to 25 (or 50 if overrideSymbolCount=true)
 * - detailFlag (query, optional): Determines market fields returned (ALL, FUNDAMENTAL, INTRADAY, OPTIONS, WEEK_52, MF_DETAIL)
 * - requireEarningsDate (query, optional): If true, nextEarningDate will be provided
 * - overrideSymbolCount (query, optional): If true, symbolList may contain up to 50 symbols
 * - skipMiniOptionsCheck (query, optional): If true, no call is made to check mini options
 */
public class GetQuotesRequest {

  @NotBlank(message = "Symbols are required")
  @Size(max = 1000, message = "Symbols string too long")
  private String symbols; // Comma-separated symbols

  private String detailFlag; // ALL, FUNDAMENTAL, INTRADAY, OPTIONS, WEEK_52, MF_DETAIL
  private Boolean requireEarningsDate;
  private Boolean overrideSymbolCount;
  private Boolean skipMiniOptionsCheck;

  public GetQuotesRequest() {
  }

  public String getSymbols() {
    return symbols;
  }

  public void setSymbols(String symbols) {
    this.symbols = symbols;
  }

  public String getDetailFlag() {
    return detailFlag;
  }

  public void setDetailFlag(String detailFlag) {
    this.detailFlag = detailFlag;
  }

  public Boolean getRequireEarningsDate() {
    return requireEarningsDate;
  }

  public void setRequireEarningsDate(Boolean requireEarningsDate) {
    this.requireEarningsDate = requireEarningsDate;
  }

  public Boolean getOverrideSymbolCount() {
    return overrideSymbolCount;
  }

  public void setOverrideSymbolCount(Boolean overrideSymbolCount) {
    this.overrideSymbolCount = overrideSymbolCount;
  }

  public Boolean getSkipMiniOptionsCheck() {
    return skipMiniOptionsCheck;
  }

  public void setSkipMiniOptionsCheck(Boolean skipMiniOptionsCheck) {
    this.skipMiniOptionsCheck = skipMiniOptionsCheck;
  }
}
