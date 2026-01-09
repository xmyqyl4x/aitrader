package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myqyl.aitradex.api.dto.EtradeAccountDto;
import com.myqyl.aitradex.etrade.accounts.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAccountAPI;
import com.myqyl.aitradex.etrade.service.EtradeAccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE Accounts API endpoints.
 * 
 * These tests validate our Accounts REST API endpoints by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying Accounts API client
 * - Validating request/response handling, error handling, etc.
 */
@DisplayName("E*TRADE Accounts API Integration Tests")
class EtradeAccountsApiIntegrationTest extends EtradeApiIntegrationTestBase {

  @MockBean
  private EtradeAccountService accountService;

  @MockBean
  private EtradeApiClientAccountAPI accountsApi;

  @BeforeEach
  void setUpAccounts() {
    // Additional setup for Accounts tests if needed
  }

  @Test
  @DisplayName("GET /api/etrade/accounts/{accountId}/balance should return balance")
  void getBalance_shouldReturnBalance() throws Exception {
    UUID accountId = testAccountId;
    EtradeAccountDto accountDto = new EtradeAccountDto(
        accountId, testUserId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), null);

    BalanceResponse balanceResponse = new BalanceResponse();
    balanceResponse.setAccountId("840104290");
    balanceResponse.setAccountType("INDIVIDUAL");
    balanceResponse.setAccountMode("MARGIN");

    CashBalance cash = new CashBalance();
    cash.setCashBalance(10000.0);
    cash.setCashAvailable(9500.0);
    balanceResponse.setCash(cash);

    MarginBalance margin = new MarginBalance();
    margin.setMarginBalance(20000.0);
    margin.setMarginAvailable(19000.0);
    balanceResponse.setMargin(margin);

    ComputedBalance computed = new ComputedBalance();
    computed.setTotal(30000.0);
    computed.setNetValue(29000.0);
    balanceResponse.setComputed(computed);

    when(accountService.getAccount(accountId)).thenReturn(accountDto);
    when(accountService.getAccountBalance(eq(accountId), eq(testAccountIdKey), 
                                          anyString(), anyString(), anyBoolean()))
        .thenReturn(balanceResponse);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", accountId)
            .param("instType", "BROKERAGE")
            .param("realTimeNAV", "true"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accountId").value("840104290"))
        .andExpect(jsonPath("$.accountType").value("INDIVIDUAL"))
        .andExpect(jsonPath("$.accountMode").value("MARGIN"))
        .andExpect(jsonPath("$.cash.cashBalance").value(10000.0))
        .andExpect(jsonPath("$.cash.cashAvailable").value(9500.0))
        .andExpect(jsonPath("$.margin.marginBalance").value(20000.0))
        .andExpect(jsonPath("$.computed.total").value(30000.0))
        .andExpect(jsonPath("$.computed.netValue").value(29000.0));

    verify(accountService, times(1)).getAccount(accountId);
    verify(accountService, times(1)).getAccountBalance(eq(accountId), eq(testAccountIdKey),
                                                       anyString(), anyString(), anyBoolean());
  }

  @Test
  @DisplayName("GET /api/etrade/accounts/{accountId}/portfolio should return portfolio")
  void getPortfolio_shouldReturnPortfolio() throws Exception {
    UUID accountId = testAccountId;
    EtradeAccountDto accountDto = new EtradeAccountDto(
        accountId, testUserId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), null);

    PortfolioResponse portfolioResponse = new PortfolioResponse();
    portfolioResponse.setTotalPages(1);

    AccountPortfolioDto accountPortfolio = new AccountPortfolioDto();
    accountPortfolio.setAccountId("840104290");
    accountPortfolio.setTotalPages(1);

    PositionDto position = new PositionDto();
    position.setPositionId(10087531L);
    position.setQuantity(100.0);
    position.setMarketValue(15000.0);
    position.setPositionType("LONG");

    ProductDto product = new ProductDto();
    product.setSymbol("AAPL");
    product.setSecurityType("EQ");
    position.setProduct(product);

    QuickViewDto quick = new QuickViewDto();
    quick.setLastTrade(150.0);
    quick.setChange(2.0);
    position.setQuick(quick);

    List<PositionDto> positions = new ArrayList<>();
    positions.add(position);
    accountPortfolio.setPositions(positions);

    List<AccountPortfolioDto> portfolios = new ArrayList<>();
    portfolios.add(accountPortfolio);
    portfolioResponse.setAccountPortfolios(portfolios);

    when(accountService.getAccount(accountId)).thenReturn(accountDto);
    when(accountService.getAccountPortfolio(eq(accountId), eq(testAccountIdKey),
                                            any(), anyString(), anyString(), any(),
                                            anyString(), anyBoolean(), anyBoolean(), anyString()))
        .thenReturn(portfolioResponse);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/portfolio", accountId)
            .param("count", "25")
            .param("sortBy", "SYMBOL"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.accountPortfolios").isArray())
        .andExpect(jsonPath("$.accountPortfolios[0].accountId").value("840104290"))
        .andExpect(jsonPath("$.accountPortfolios[0].positions").isArray())
        .andExpect(jsonPath("$.accountPortfolios[0].positions[0].positionId").value(10087531))
        .andExpect(jsonPath("$.accountPortfolios[0].positions[0].quantity").value(100.0))
        .andExpect(jsonPath("$.accountPortfolios[0].positions[0].product.symbol").value("AAPL"))
        .andExpect(jsonPath("$.accountPortfolios[0].positions[0].quick.lastTrade").value(150.0));

    verify(accountService, times(1)).getAccount(accountId);
    verify(accountService, times(1)).getAccountPortfolio(eq(accountId), eq(testAccountIdKey),
                                                         any(), anyString(), anyString(), any(),
                                                         anyString(), anyBoolean(), anyBoolean(), anyString());
  }

  @Test
  @DisplayName("GET /api/etrade/accounts/{accountId}/balance should handle service errors")
  void getBalance_shouldHandleServiceErrors() throws Exception {
    UUID accountId = testAccountId;
    EtradeAccountDto accountDto = new EtradeAccountDto(
        accountId, testUserId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), null);

    when(accountService.getAccount(accountId)).thenReturn(accountDto);
    when(accountService.getAccountBalance(eq(accountId), eq(testAccountIdKey),
                                          anyString(), anyString(), anyBoolean()))
        .thenThrow(new RuntimeException("Failed to get balance"));

    mockMvc.perform(get("/api/etrade/accounts/{accountId}/balance", accountId))
        .andExpect(status().is5xxServerError());

    verify(accountService, times(1)).getAccount(accountId);
  }

  @Test
  @DisplayName("POST /api/etrade/accounts/sync should sync accounts")
  void syncAccounts_shouldSyncAccounts() throws Exception {
    UUID userId = testUserId;
    UUID accountId = testAccountId;

    List<EtradeAccountDto> syncedAccounts = new ArrayList<>();
    EtradeAccountDto accountDto = new EtradeAccountDto(
        testAccountId, userId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
    syncedAccounts.add(accountDto);

    when(accountService.syncAccounts(userId, accountId)).thenReturn(syncedAccounts);

    mockMvc.perform(post("/api/etrade/accounts/sync")
            .param("userId", userId.toString())
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].accountIdKey").value(testAccountIdKey))
        .andExpect(jsonPath("$[0].accountType").value("INDIVIDUAL"));

    verify(accountService, times(1)).syncAccounts(userId, accountId);
  }

  @Test
  @DisplayName("GET /api/etrade/accounts should return user accounts")
  void getUserAccounts_shouldReturnAccounts() throws Exception {
    UUID userId = testUserId;

    List<EtradeAccountDto> accounts = new ArrayList<>();
    EtradeAccountDto accountDto = new EtradeAccountDto(
        testAccountId, userId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), null);
    accounts.add(accountDto);

    when(accountService.getUserAccounts(userId)).thenReturn(accounts);

    mockMvc.perform(get("/api/etrade/accounts")
            .param("userId", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].accountIdKey").value(testAccountIdKey));

    verify(accountService, times(1)).getUserAccounts(userId);
  }

  @Test
  @DisplayName("GET /api/etrade/accounts/{accountId} should return account details")
  void getAccount_shouldReturnAccountDetails() throws Exception {
    UUID accountId = testAccountId;
    EtradeAccountDto accountDto = new EtradeAccountDto(
        accountId, testUserId, testAccountIdKey, "INDIVIDUAL", "Test Account", "ACTIVE",
        java.time.OffsetDateTime.now(), null);

    when(accountService.getAccount(accountId)).thenReturn(accountDto);

    mockMvc.perform(get("/api/etrade/accounts/{accountId}", accountId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(accountId.toString()))
        .andExpect(jsonPath("$.accountIdKey").value(testAccountIdKey))
        .andExpect(jsonPath("$.accountType").value("INDIVIDUAL"));

    verify(accountService, times(1)).getAccount(accountId);
  }
}
