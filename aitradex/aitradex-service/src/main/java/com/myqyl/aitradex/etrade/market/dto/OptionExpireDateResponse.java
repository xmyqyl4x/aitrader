package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Get Option Expire Dates API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains list of expiration dates for options.
 */
public class OptionExpireDateResponse {

  @Valid
  private List<OptionExpireDateDto> expireDates;

  public OptionExpireDateResponse() {
    this.expireDates = new ArrayList<>();
  }

  public List<OptionExpireDateDto> getExpireDates() {
    return expireDates;
  }

  public void setExpireDates(List<OptionExpireDateDto> expireDates) {
    this.expireDates = expireDates != null ? expireDates : new ArrayList<>();
  }
}
