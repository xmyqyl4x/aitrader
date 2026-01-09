package com.myqyl.aitradex.etrade.orders.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PreviewId DTO from Preview Order Response.
 * 
 * According to E*TRADE Order API documentation:
 * Contains the preview ID used for placing an order.
 */
public class PreviewIdDto {

  @NotBlank(message = "Preview ID is required")
  private String previewId;

  public PreviewIdDto() {
  }

  public PreviewIdDto(String previewId) {
    this.previewId = previewId;
  }

  public String getPreviewId() {
    return previewId;
  }

  public void setPreviewId(String previewId) {
    this.previewId = previewId;
  }
}
