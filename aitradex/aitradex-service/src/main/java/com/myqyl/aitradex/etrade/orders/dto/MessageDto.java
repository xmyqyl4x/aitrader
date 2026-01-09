package com.myqyl.aitradex.etrade.orders.dto;

/**
 * Message DTO for Order API responses.
 * 
 * According to E*TRADE Order API documentation:
 * Represents error or informational messages from order operations.
 */
public class MessageDto {

  private String type; // ERROR, INFO, WARNING
  private String code; // Error code
  private String description; // Error description

  public MessageDto() {
  }

  public MessageDto(String type, String code, String description) {
    this.type = type;
    this.code = code;
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
