package com.myqyl.aitradex.etrade.market.dto;

/**
 * Selected Expiration Date DTO for Quote.
 * 
 * According to E*TRADE Market API documentation:
 * Represents the selected expiration date (month, year, day).
 */
public class SelectedEDDto {

  private Integer month;
  private Integer year;
  private Integer day;

  public SelectedEDDto() {
  }

  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public Integer getDay() {
    return day;
  }

  public void setDay(Integer day) {
    this.day = day;
  }
}
