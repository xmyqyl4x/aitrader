package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountListResponse DTO.
 */
@DisplayName("AccountListResponse DTO Tests")
class AccountListResponseTest {

  @Test
  @DisplayName("Should create AccountListResponse with accounts")
  void shouldCreateWithAccounts() {
    List<EtradeAccountModel> accountList = new ArrayList<>();
    EtradeAccountModel account1 = new EtradeAccountModel("account1", "key1", "INDIVIDUAL");
    EtradeAccountModel account2 = new EtradeAccountModel("account2", "key2", "JOINT");
    accountList.add(account1);
    accountList.add(account2);

    AccountListResponse response = new AccountListResponse(accountList);

    assertNotNull(response.getAccounts());
    assertEquals(2, response.getAccountList().size());
    assertEquals("account1", response.getAccountList().get(0).getAccountId());
    assertEquals("account2", response.getAccountList().get(1).getAccountId());
  }

  @Test
  @DisplayName("Should create AccountListResponse with default constructor")
  void shouldCreateWithDefaultConstructor() {
    AccountListResponse response = new AccountListResponse();

    assertNotNull(response.getAccounts());
    assertTrue(response.getAccountList().isEmpty());
  }

  @Test
  @DisplayName("Should handle null account list gracefully")
  void shouldHandleNullAccountList() {
    AccountListResponse response = new AccountListResponse((List<EtradeAccountModel>) null);

    assertNotNull(response.getAccounts());
    assertTrue(response.getAccountList().isEmpty());
  }

  @Test
  @DisplayName("Should get account list directly")
  void shouldGetAccountListDirectly() {
    List<EtradeAccountModel> accountList = new ArrayList<>();
    accountList.add(new EtradeAccountModel("account1", "key1", "INDIVIDUAL"));

    AccountListResponse response = new AccountListResponse(accountList);

    assertEquals(1, response.getAccountList().size());
    assertEquals("account1", response.getAccountList().get(0).getAccountId());
  }
}
