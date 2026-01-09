package com.myqyl.aitradex.etrade.market.dto;

/**
 * Quote Model DTO for E*TRADE Market API responses.
 * 
 * According to E*TRADE Market API documentation:
 * Represents a single quote with all its details.
 * Contains AllQuoteDetailsDto which includes all fields based on detailFlag.
 */
public class EtradeQuoteModel {

  private AllQuoteDetailsDto all;
  private AllQuoteDetailsDto fundamental; // FUNDAMENTAL detailFlag
  private AllQuoteDetailsDto intraday; // INTRADAY detailFlag
  private AllQuoteDetailsDto options; // OPTIONS detailFlag
  private AllQuoteDetailsDto week52; // WEEK_52 detailFlag
  private AllQuoteDetailsDto mutualFund; // MF_DETAIL detailFlag

  public EtradeQuoteModel() {
  }

  /**
   * Gets quote details based on what was requested.
   * The API returns different fields based on detailFlag.
   * This method returns the appropriate section or defaults to 'all'.
   */
  public AllQuoteDetailsDto getQuoteDetails() {
    if (all != null) {
      return all;
    }
    if (intraday != null) {
      return intraday;
    }
    if (fundamental != null) {
      return fundamental;
    }
    if (options != null) {
      return options;
    }
    if (week52 != null) {
      return week52;
    }
    if (mutualFund != null) {
      return mutualFund;
    }
    return null;
  }

  public AllQuoteDetailsDto getAll() {
    return all;
  }

  public void setAll(AllQuoteDetailsDto all) {
    this.all = all;
  }

  public AllQuoteDetailsDto getFundamental() {
    return fundamental;
  }

  public void setFundamental(AllQuoteDetailsDto fundamental) {
    this.fundamental = fundamental;
  }

  public AllQuoteDetailsDto getIntraday() {
    return intraday;
  }

  public void setIntraday(AllQuoteDetailsDto intraday) {
    this.intraday = intraday;
  }

  public AllQuoteDetailsDto getOptions() {
    return options;
  }

  public void setOptions(AllQuoteDetailsDto options) {
    this.options = options;
  }

  public AllQuoteDetailsDto getWeek52() {
    return week52;
  }

  public void setWeek52(AllQuoteDetailsDto week52) {
    this.week52 = week52;
  }

  public AllQuoteDetailsDto getMutualFund() {
    return mutualFund;
  }

  public void setMutualFund(AllQuoteDetailsDto mutualFund) {
    this.mutualFund = mutualFund;
  }
}
