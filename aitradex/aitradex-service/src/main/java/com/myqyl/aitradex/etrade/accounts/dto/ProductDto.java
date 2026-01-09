package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Product DTO from Position/Portfolio.
 * 
 * According to E*TRADE Accounts API documentation:
 * Represents a financial product (equity, option, mutual fund, etc.).
 */
public class ProductDto {

  private String symbol;
  private String securityType; // BOND, EQ, INDX, MF, MMF, OPTN
  private String securitySubType;
  private String callPut; // CALL, PUT (for options)
  private Integer expiryYear; // int32
  private Integer expiryMonth; // int32 (1-12)
  private Integer expiryDay; // int32 (1-31)
  private Double strikePrice;
  private String expiryType;
  private ProductIdDto productId;

  public ProductDto() {
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getSecurityType() {
    return securityType;
  }

  public void setSecurityType(String securityType) {
    this.securityType = securityType;
  }

  public String getSecuritySubType() {
    return securitySubType;
  }

  public void setSecuritySubType(String securitySubType) {
    this.securitySubType = securitySubType;
  }

  public String getCallPut() {
    return callPut;
  }

  public void setCallPut(String callPut) {
    this.callPut = callPut;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryDay() {
    return expiryDay;
  }

  public void setExpiryDay(Integer expiryDay) {
    this.expiryDay = expiryDay;
  }

  public Double getStrikePrice() {
    return strikePrice;
  }

  public void setStrikePrice(Double strikePrice) {
    this.strikePrice = strikePrice;
  }

  public String getExpiryType() {
    return expiryType;
  }

  public void setExpiryType(String expiryType) {
    this.expiryType = expiryType;
  }

  public ProductIdDto getProductId() {
    return productId;
  }

  public void setProductId(ProductIdDto productId) {
    this.productId = productId;
  }

  /**
   * ProductId nested DTO.
   */
  public static class ProductIdDto {
    private String symbol;
    private String typeCode; // EQUITY, OPTION, MUTUAL_FUND, INDEX, etc.

    public ProductIdDto() {
    }

    public String getSymbol() {
      return symbol;
    }

    public void setSymbol(String symbol) {
      this.symbol = symbol;
    }

    public String getTypeCode() {
      return typeCode;
    }

    public void setTypeCode(String typeCode) {
      this.typeCode = typeCode;
    }
  }
}
