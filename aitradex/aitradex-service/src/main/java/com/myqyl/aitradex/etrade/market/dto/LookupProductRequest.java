package com.myqyl.aitradex.etrade.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Lookup Product API.
 * 
 * According to E*TRADE Market API documentation:
 * Query parameters:
 * - input (path, required): Symbol or company name to search for
 */
public class LookupProductRequest {

  @NotBlank(message = "Input is required")
  @Size(min = 1, max = 100, message = "Input must be between 1 and 100 characters")
  private String input;

  public LookupProductRequest() {
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }
}
