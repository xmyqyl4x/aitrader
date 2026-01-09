package com.myqyl.aitradex.etrade.accounts.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Account model DTO representing an E*TRADE account from the Accounts API.
 * 
 * According to E*TRADE Accounts API documentation:
 * All fields from the Account model are included.
 * 
 * Note: This is different from com.myqyl.aitradex.api.dto.AccountDto which is for internal account management.
 */
public class EtradeAccountModel {

  private Integer accountNo;

  @NotBlank(message = "Account ID is required")
  private String accountId;

  @NotBlank(message = "Account ID key is required")
  private String accountIdKey;

  private String accountMode; // CASH, MARGIN, CHECKING, IRA, SAVINGS, CD

  private String accountDesc;

  private String accountName;

  @NotBlank(message = "Account type is required")
  private String accountType; // AMMCHK, ARO, BCHK, BENFIRA, etc. (see documentation)

  private String institutionType; // BROKERAGE

  private String accountStatus; // ACTIVE, CLOSED

  private Long closedDate; // int64 - date when account was closed

  private Boolean shareWorksAccount; // Is Shareworks Account?

  private String shareWorksSource; // Shareworks Source

  private Boolean fCManagedMssbClosedAccount;

  public EtradeAccountModel() {
  }

  public EtradeAccountModel(String accountId, String accountIdKey, String accountType) {
    this.accountId = accountId;
    this.accountIdKey = accountIdKey;
    this.accountType = accountType;
  }

  // Getters and Setters

  public Integer getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(Integer accountNo) {
    this.accountNo = accountNo;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountIdKey() {
    return accountIdKey;
  }

  public void setAccountIdKey(String accountIdKey) {
    this.accountIdKey = accountIdKey;
  }

  public String getAccountMode() {
    return accountMode;
  }

  public void setAccountMode(String accountMode) {
    this.accountMode = accountMode;
  }

  public String getAccountDesc() {
    return accountDesc;
  }

  public void setAccountDesc(String accountDesc) {
    this.accountDesc = accountDesc;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public String getInstitutionType() {
    return institutionType;
  }

  public void setInstitutionType(String institutionType) {
    this.institutionType = institutionType;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }

  public Long getClosedDate() {
    return closedDate;
  }

  public void setClosedDate(Long closedDate) {
    this.closedDate = closedDate;
  }

  public Boolean getShareWorksAccount() {
    return shareWorksAccount;
  }

  public void setShareWorksAccount(Boolean shareWorksAccount) {
    this.shareWorksAccount = shareWorksAccount;
  }

  public String getShareWorksSource() {
    return shareWorksSource;
  }

  public void setShareWorksSource(String shareWorksSource) {
    this.shareWorksSource = shareWorksSource;
  }

  public Boolean getFCManagedMssbClosedAccount() {
    return fCManagedMssbClosedAccount;
  }

  public void setFCManagedMssbClosedAccount(Boolean fCManagedMssbClosedAccount) {
    this.fCManagedMssbClosedAccount = fCManagedMssbClosedAccount;
  }
}
