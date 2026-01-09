package com.myqyl.aitradex.etrade.accounts.dto;

/**
 * Request DTO for Get Account Balances API.
 * 
 * According to E*TRADE Accounts API documentation:
 * Query parameters:
 * - instType (query, optional): Institution type (default: "BROKERAGE")
 * - accountType (query, optional): The registered account type
 * - realTimeNAV (query, optional): Whether to get real-time NAV (default: true)
 */
public class BalanceRequest {

  private String instType = "BROKERAGE";
  private String accountType;
  private Boolean realTimeNAV = true;

  public BalanceRequest() {
  }

  public BalanceRequest(String instType, String accountType, Boolean realTimeNAV) {
    this.instType = instType != null ? instType : "BROKERAGE";
    this.accountType = accountType;
    this.realTimeNAV = realTimeNAV != null ? realTimeNAV : true;
  }

  public String getInstType() {
    return instType;
  }

  public void setInstType(String instType) {
    this.instType = instType != null ? instType : "BROKERAGE";
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public Boolean getRealTimeNAV() {
    return realTimeNAV;
  }

  public void setRealTimeNAV(Boolean realTimeNAV) {
    this.realTimeNAV = realTimeNAV != null ? realTimeNAV : true;
  }
}
