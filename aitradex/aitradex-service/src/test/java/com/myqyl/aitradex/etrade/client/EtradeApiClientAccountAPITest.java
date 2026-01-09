package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.accounts.dto.*;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeApiClientAccountAPI.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeApiClientAccountAPI Tests")
class EtradeApiClientAccountAPITest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeApiClientAccountAPI accountsApi;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
  private static final String TEST_ACCOUNT_ID_KEY = "JIdOIAcSpwR1Jva7RQBraQ";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    accountsApi = new EtradeApiClientAccountAPI(apiClient, properties, objectMapper);

    when(properties.getAccountsListUrl())
        .thenReturn("https://apisb.etrade.com/v1/accounts/list");
    when(properties.getBalanceUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/balance");
    when(properties.getPortfolioUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/portfolio");
  }

  @Test
  @DisplayName("listAccounts - Success")
  void listAccounts_shouldReturnAccountList() throws Exception {
    // Convert XML to JSON for ObjectMapper
    String responseJson = "{\"AccountListResponse\":{\"Accounts\":{\"Account\":{\"accountId\":\"840104290\"," +
        "\"accountIdKey\":\"" + TEST_ACCOUNT_ID_KEY + "\",\"accountMode\":\"MARGIN\",\"accountDesc\":\"INDIVIDUAL\"," +
        "\"accountName\":\"Individual Brokerage\",\"accountType\":\"INDIVIDUAL\"," +
        "\"institutionType\":\"BROKERAGE\",\"accountStatus\":\"ACTIVE\"}}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    AccountListResponse result = accountsApi.listAccounts(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertNotNull(result.getAccounts());
    assertEquals(1, result.getAccountList().size());
    EtradeAccountModel account = result.getAccountList().get(0);
    assertEquals("840104290", account.getAccountId());
    assertEquals(TEST_ACCOUNT_ID_KEY, account.getAccountIdKey());
    assertEquals("MARGIN", account.getAccountMode());
    assertEquals("INDIVIDUAL", account.getAccountType());
    assertEquals("BROKERAGE", account.getInstitutionType());
    assertEquals("ACTIVE", account.getAccountStatus());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("listAccounts - Handles multiple accounts")
  void listAccounts_shouldHandleMultipleAccounts() throws Exception {
    String responseJson = "{\"AccountListResponse\":{\"Accounts\":{\"Account\":[" +
        "{\"accountId\":\"840104290\",\"accountIdKey\":\"" + TEST_ACCOUNT_ID_KEY + "\"," +
        "\"accountType\":\"INDIVIDUAL\",\"accountStatus\":\"ACTIVE\"}," +
        "{\"accountId\":\"840104291\",\"accountIdKey\":\"Key2\",\"accountType\":\"JOINT\"," +
        "\"accountStatus\":\"ACTIVE\"}]}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    AccountListResponse result = accountsApi.listAccounts(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertEquals(2, result.getAccountList().size());
  }

  @Test
  @DisplayName("listAccounts - Handles empty response")
  void listAccounts_shouldHandleEmptyResponse() throws Exception {
    String responseJson = "{\"AccountListResponse\":{\"Accounts\":{}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    AccountListResponse result = accountsApi.listAccounts(TEST_ACCOUNT_ID);

    assertNotNull(result);
    assertEquals(0, result.getAccountList().size());
  }

  @Test
  @DisplayName("listAccounts - Throws exception on API error")
  void listAccounts_shouldThrowExceptionOnError() throws Exception {
    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenThrow(new EtradeApiException(500, "API_ERROR", "Failed to get accounts"));

    assertThrows(EtradeApiException.class, () -> accountsApi.listAccounts(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("getAccountBalance - Success")
  void getAccountBalance_shouldReturnBalance() throws Exception {
    String responseJson = "{\"BalanceResponse\":{" +
        "\"accountId\":\"840104290\"," +
        "\"accountType\":\"INDIVIDUAL\"," +
        "\"accountDescription\":\"Individual Brokerage\"," +
        "\"accountMode\":\"MARGIN\"," +
        "\"Cash\":{" +
        "\"cashBalance\":10000.0," +
        "\"cashAvailable\":9500.0" +
        "}," +
        "\"Margin\":{" +
        "\"marginBalance\":20000.0," +
        "\"marginAvailable\":19000.0" +
        "}," +
        "\"Computed\":{" +
        "\"total\":30000.0," +
        "\"netValue\":29000.0" +
        "}" +
        "}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    BalanceRequest request = new BalanceRequest();
    BalanceResponse result = accountsApi.getAccountBalance(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertEquals("840104290", result.getAccountId());
    assertEquals("INDIVIDUAL", result.getAccountType());
    assertNotNull(result.getCash());
    assertEquals(10000.0, result.getCash().getCashBalance());
    assertNotNull(result.getMargin());
    assertEquals(20000.0, result.getMargin().getMarginBalance());
    assertNotNull(result.getComputed());
    assertEquals(30000.0, result.getComputed().getTotal());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("viewPortfolio - Success")
  void viewPortfolio_shouldReturnPortfolio() throws Exception {
    String responseJson = "{\"PortfolioResponse\":{" +
        "\"totalPages\":1," +
        "\"AccountPortfolio\":{" +
        "\"accountId\":\"840104290\"," +
        "\"totalPages\":1," +
        "\"Position\":[" +
        "{" +
        "\"positionId\":10087531," +
        "\"Product\":{\"symbol\":\"AAPL\",\"securityType\":\"EQ\"}," +
        "\"quantity\":100.0," +
        "\"marketValue\":15000.0," +
        "\"positionType\":\"LONG\"," +
        "\"Quick\":{\"lastTrade\":150.0,\"change\":2.0}" +
        "}" +
        "]" +
        "}" +
        "}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PortfolioRequest request = new PortfolioRequest();
    PortfolioResponse result = accountsApi.viewPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertEquals(1, result.getTotalPages());
    assertEquals(1, result.getAccountPortfolios().size());
    AccountPortfolioDto portfolio = result.getAccountPortfolios().get(0);
    assertEquals("840104290", portfolio.getAccountId());
    assertEquals(1, portfolio.getPositions().size());
    PositionDto position = portfolio.getPositions().get(0);
    assertEquals(Long.valueOf(10087531), position.getPositionId());
    assertEquals(100.0, position.getQuantity());
    assertEquals(15000.0, position.getMarketValue());
    assertEquals("LONG", position.getPositionType());
    assertNotNull(position.getProduct());
    assertEquals("AAPL", position.getProduct().getSymbol());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("viewPortfolio - Handles empty positions")
  void viewPortfolio_shouldHandleEmptyPositions() throws Exception {
    String responseJson = "{\"PortfolioResponse\":{" +
        "\"AccountPortfolio\":{\"accountId\":\"840104290\"}" +
        "}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PortfolioRequest request = new PortfolioRequest();
    PortfolioResponse result = accountsApi.viewPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertEquals(1, result.getAccountPortfolios().size());
    assertTrue(result.getAccountPortfolios().get(0).getPositions().isEmpty());
  }

  @Test
  @DisplayName("viewPortfolio - Throws exception on API error")
  void viewPortfolio_shouldThrowExceptionOnError() throws Exception {
    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenThrow(new EtradeApiException(500, "API_ERROR", "Failed to get portfolio"));

    PortfolioRequest request = new PortfolioRequest();
    assertThrows(EtradeApiException.class, 
        () -> accountsApi.viewPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request));
  }
}
