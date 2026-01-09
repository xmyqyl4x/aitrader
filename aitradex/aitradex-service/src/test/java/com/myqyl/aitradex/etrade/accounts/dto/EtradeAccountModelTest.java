package com.myqyl.aitradex.etrade.accounts.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EtradeAccountModel DTO.
 */
@DisplayName("EtradeAccountModel DTO Tests")
class EtradeAccountModelTest {

  @Test
  @DisplayName("Should create EtradeAccountModel with required fields")
  void shouldCreateWithRequiredFields() {
    EtradeAccountModel account = new EtradeAccountModel("account123", "accountKey123", "INDIVIDUAL");

    assertEquals("account123", account.getAccountId());
    assertEquals("accountKey123", account.getAccountIdKey());
    assertEquals("INDIVIDUAL", account.getAccountType());
  }

  @Test
  @DisplayName("Should create EtradeAccountModel with default constructor")
  void shouldCreateWithDefaultConstructor() {
    EtradeAccountModel account = new EtradeAccountModel();

    assertNull(account.getAccountId());
    assertNull(account.getAccountIdKey());
    assertNull(account.getAccountType());
  }

  @Test
  @DisplayName("Should set and get all fields")
  void shouldSetAndGetAllFields() {
    EtradeAccountModel account = new EtradeAccountModel();

    account.setAccountNo(12345);
    account.setAccountId("account123");
    account.setAccountIdKey("accountKey123");
    account.setAccountMode("MARGIN");
    account.setAccountDesc("Individual Brokerage");
    account.setAccountName("My Account");
    account.setAccountType("INDIVIDUAL");
    account.setInstitutionType("BROKERAGE");
    account.setAccountStatus("ACTIVE");
    account.setClosedDate(1234567890L);
    account.setShareWorksAccount(true);
    account.setShareWorksSource("SOURCE");
    account.setFCManagedMssbClosedAccount(false);

    assertEquals(Integer.valueOf(12345), account.getAccountNo());
    assertEquals("account123", account.getAccountId());
    assertEquals("accountKey123", account.getAccountIdKey());
    assertEquals("MARGIN", account.getAccountMode());
    assertEquals("Individual Brokerage", account.getAccountDesc());
    assertEquals("My Account", account.getAccountName());
    assertEquals("INDIVIDUAL", account.getAccountType());
    assertEquals("BROKERAGE", account.getInstitutionType());
    assertEquals("ACTIVE", account.getAccountStatus());
    assertEquals(Long.valueOf(1234567890L), account.getClosedDate());
    assertTrue(account.getShareWorksAccount());
    assertEquals("SOURCE", account.getShareWorksSource());
    assertFalse(account.getFCManagedMssbClosedAccount());
  }
}
