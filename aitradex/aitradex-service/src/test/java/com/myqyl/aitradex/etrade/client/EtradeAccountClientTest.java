package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.client.EtradeApiClient;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeAccountClient.
 */
@ExtendWith(MockitoExtension.class)
class EtradeAccountClientTest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeAccountClient accountClient;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
  private static final String TEST_ACCOUNT_ID_KEY = "12345678";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    accountClient = new EtradeAccountClient(apiClient, properties, objectMapper);

    when(properties.getAccountsListUrl())
        .thenReturn("https://apisb.etrade.com/v1/accounts/list");
    when(properties.getBalanceUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/balance");
    when(properties.getPortfolioUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/portfolio");
  }

  @Test
  void getAccountList_returnsAccountList() throws Exception {
    String mockResponse = """
        {
          "AccountListResponse": {
            "Accounts": {
              "Account": [
                {
                  "accountId": "12345678",
                  "accountIdKey": "KEY123",
                  "accountName": "Test Account",
                  "accountType": "BROKERAGE",
                  "accountDesc": "Individual Brokerage",
                  "accountStatus": "ACTIVE"
                }
              ]
            }
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> accounts = accountClient.getAccountList(TEST_ACCOUNT_ID);

    assertNotNull(accounts);
    assertFalse(accounts.isEmpty());
    assertEquals("12345678", accounts.get(0).get("accountId"));
    assertEquals("KEY123", accounts.get(0).get("accountIdKey"));
    assertEquals("Test Account", accounts.get(0).get("accountName"));
  }

  @Test
  void getAccountList_singleAccount_handlesCorrectly() throws Exception {
    String mockResponse = """
        {
          "AccountListResponse": {
            "Accounts": {
              "Account": {
                "accountId": "12345678",
                "accountIdKey": "KEY123",
                "accountName": "Single Account",
                "accountType": "BROKERAGE",
                "accountStatus": "ACTIVE"
              }
            }
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> accounts = accountClient.getAccountList(TEST_ACCOUNT_ID);

    assertNotNull(accounts);
    assertEquals(1, accounts.size());
    assertEquals("Single Account", accounts.get(0).get("accountName"));
  }

  @Test
  void getBalance_returnsBalance() throws Exception {
    String mockResponse = """
        {
          "BalanceResponse": {
            "accountId": "12345678",
            "accountType": "BROKERAGE",
            "Computed": {
              "total": 50000.00,
              "netCash": 25000.00,
              "cashAvailableForInvestment": 25000.00
            }
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> balance = accountClient.getBalance(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(balance);
    assertEquals("12345678", balance.get("accountId"));
    assertTrue(balance.containsKey("computed"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> computed = (Map<String, Object>) balance.get("computed");
    assertNotNull(computed);
    assertEquals("50000.00", computed.get("total"));
  }

  @Test
  void getPortfolio_returnsPortfolio() throws Exception {
    String mockResponse = """
        {
          "PortfolioResponse": {
            "AccountPortfolio": [{
              "accountId": "12345678",
              "Position": [{
                "Product": {
                  "symbol": "AAPL",
                  "securityType": "EQ",
                  "exchange": "NASDAQ"
                },
                "quantity": 100,
                "pricePaid": 150.00,
                "marketValue": 15180.00,
                "totalGain": 180.00,
                "totalGainPct": 1.2,
                "Quick": {
                  "lastTrade": 151.80
                }
              }]
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> portfolio = accountClient.getPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(portfolio);
    assertTrue(portfolio.containsKey("positions"));
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> positions = (List<Map<String, Object>>) portfolio.get("positions");
    assertNotNull(positions);
    assertFalse(positions.isEmpty());
    assertEquals("AAPL", positions.get(0).get("symbol"));
    assertEquals(100.0, positions.get(0).get("quantity"));
  }

  @Test
  void getPortfolio_emptyPositions_returnsEmptyList() throws Exception {
    String mockResponse = """
        {
          "PortfolioResponse": {
            "AccountPortfolio": [{
              "accountId": "12345678",
              "Position": []
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> portfolio = accountClient.getPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(portfolio);
    assertTrue(portfolio.containsKey("positions"));
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> positions = (List<Map<String, Object>>) portfolio.get("positions");
    assertNotNull(positions);
    assertTrue(positions.isEmpty());
  }

  @Test
  void getPortfolio_fallbackToDirectPositionArray_handlesCorrectly() throws Exception {
    // Fallback: direct Position array (older structure)
    String mockResponse = """
        {
          "PortfolioResponse": {
            "accountId": "12345678",
            "Position": [{
              "Product": {
                "symbol": "MSFT",
                "securityType": "EQ"
              },
              "quantity": 50,
              "pricePaid": 300.00,
              "marketValue": 15000.00
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> portfolio = accountClient.getPortfolio(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(portfolio);
    assertTrue(portfolio.containsKey("positions"));
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> positions = (List<Map<String, Object>>) portfolio.get("positions");
    assertFalse(positions.isEmpty());
    assertEquals("MSFT", positions.get(0).get("symbol"));
  }
}
