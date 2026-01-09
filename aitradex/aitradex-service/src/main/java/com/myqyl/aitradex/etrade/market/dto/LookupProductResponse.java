package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Lookup Product API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains list of products matching the search input.
 */
public class LookupProductResponse {

  @Valid
  private List<LookupProductDto> data;

  public LookupProductResponse() {
    this.data = new ArrayList<>();
  }

  public List<LookupProductDto> getData() {
    return data;
  }

  public void setData(List<LookupProductDto> data) {
    this.data = data != null ? data : new ArrayList<>();
  }
}
