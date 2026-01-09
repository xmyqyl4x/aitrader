package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Get Quotes API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains list of quotes in QuoteData array.
 */
public class QuoteResponse {

  @Valid
  private List<EtradeQuoteModel> quoteData;

  public QuoteResponse() {
    this.quoteData = new ArrayList<>();
  }

  public List<EtradeQuoteModel> getQuoteData() {
    return quoteData;
  }

  public void setQuoteData(List<EtradeQuoteModel> quoteData) {
    this.quoteData = quoteData != null ? quoteData : new ArrayList<>();
  }
}
