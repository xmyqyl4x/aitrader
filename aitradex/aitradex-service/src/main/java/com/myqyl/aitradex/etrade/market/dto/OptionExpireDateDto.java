package com.myqyl.aitradex.etrade.market.dto;

/**
 * Option Expiration Date DTO from Get Option Expire Dates API.
 * 
 * According to E*TRADE Market API documentation:
 * Contains expiration date information (year, month, day, expiryType).
 */
public class OptionExpireDateDto {

  private Integer year;
  private Integer month;
  private Integer day;
  private String expiryType; // WEEKLY, MONTHLY

  public OptionExpireDateDto() {
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public Integer getDay() {
    return day;
  }

  public void setDay(Integer day) {
    this.day = day;
  }

  public String getExpiryType() {
    return expiryType;
  }

  public void setExpiryType(String expiryType) {
    this.expiryType = expiryType;
  }
}
