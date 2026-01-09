package com.myqyl.aitradex.etrade.accounts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for List Accounts API.
 * 
 * According to E*TRADE Accounts API documentation:
 * - accounts (Accounts): List of accounts
 */
public class AccountListResponse {

  @NotNull(message = "Accounts list is required")
  @Valid
  private Accounts accounts;

  public AccountListResponse() {
    this.accounts = new Accounts();
  }

  public AccountListResponse(Accounts accounts) {
    this.accounts = accounts;
  }

  public AccountListResponse(List<EtradeAccountModel> accountList) {
    this.accounts = new Accounts(accountList);
  }

  public Accounts getAccounts() {
    return accounts;
  }

  public void setAccounts(Accounts accounts) {
    this.accounts = accounts;
  }

  /**
   * Helper method to get account list directly.
   */
  public List<EtradeAccountModel> getAccountList() {
    return accounts != null ? accounts.getAccount() : new ArrayList<>();
  }

  /**
   * Nested Accounts container.
   */
  public static class Accounts {
    private List<EtradeAccountModel> account;

    public Accounts() {
      this.account = new ArrayList<>();
    }

    public Accounts(List<EtradeAccountModel> account) {
      this.account = account != null ? account : new ArrayList<>();
    }

    public List<EtradeAccountModel> getAccount() {
      return account;
    }

    public void setAccount(List<EtradeAccountModel> account) {
      this.account = account != null ? account : new ArrayList<>();
    }
  }
}
