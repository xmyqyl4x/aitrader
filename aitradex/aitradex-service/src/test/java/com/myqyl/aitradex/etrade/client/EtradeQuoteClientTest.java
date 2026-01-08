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
 * Unit tests for EtradeQuoteClient.
 */
@ExtendWith(MockitoExtension.class)
class EtradeQuoteClientTest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeQuoteClient quoteClient;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    quoteClient = new EtradeQuoteClient(apiClient, properties, objectMapper);

    when(properties.getQuoteUrl())
        .thenReturn("https://apisb.etrade.com/v1/market/quote");
  }

  @Test
  void getQuotes_singleSymbol_returnsQuote() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": [{
              "Product": {
                "symbol": "AAPL",
                "exchange": "NASDAQ",
                "companyName": "Apple Inc."
              },
              "All": {
                "lastTrade": 151.80,
                "previousClose": 150.00,
                "open": 150.25,
                "high": 152.50,
                "low": 149.75,
                "volume": 50000000,
                "change": 1.80,
                "changePercent": 1.20,
                "bid": 151.75,
                "ask": 151.85,
                "bidSize": 100,
                "askSize": 200,
                "timeOfLastTrade": 1640995200000
              },
              "dateTime": 1640995200000
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(TEST_ACCOUNT_ID, "AAPL");

    assertNotNull(quotes);
    assertFalse(quotes.isEmpty());
    assertEquals("AAPL", quotes.get(0).get("symbol"));
    assertEquals(151.80, quotes.get(0).get("lastTrade"));
    assertEquals("Apple Inc.", quotes.get(0).get("companyName"));
  }

  @Test
  void getQuotes_multipleSymbols_returnsAllQuotes() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": [
              {
                "Product": {"symbol": "AAPL", "companyName": "Apple Inc."},
                "All": {"lastTrade": 151.80, "volume": 50000000}
              },
              {
                "Product": {"symbol": "MSFT", "companyName": "Microsoft Corp."},
                "All": {"lastTrade": 352.50, "volume": 25000000}
              }
            ]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(TEST_ACCOUNT_ID, "AAPL", "MSFT");

    assertNotNull(quotes);
    assertEquals(2, quotes.size());
    assertEquals("AAPL", quotes.get(0).get("symbol"));
    assertEquals("MSFT", quotes.get(1).get("symbol"));
  }

  @Test
  void getQuotes_singleQuoteObject_handlesCorrectly() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": {
              "Product": {"symbol": "TSLA", "companyName": "Tesla Inc."},
              "All": {"lastTrade": 250.75, "volume": 30000000}
            }
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(TEST_ACCOUNT_ID, "TSLA");

    assertNotNull(quotes);
    assertEquals(1, quotes.size());
    assertEquals("TSLA", quotes.get(0).get("symbol"));
  }

  @Test
  void getQuotes_emptyResponse_returnsEmptyList() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": []
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(TEST_ACCOUNT_ID, "INVALID");

    assertNotNull(quotes);
    assertTrue(quotes.isEmpty());
  }

  @Test
  void getQuotes_withMutualFund_parsesMutualFundFields() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": [{
              "Product": {
                "symbol": "VTSAX",
                "securityType": "MF",
                "companyName": "Vanguard Total Stock Market Index Fund"
              },
              "MutualFund": {
                "netAssetValue": 100.50,
                "publicOfferPrice": 100.50,
                "changeClose": 0.25,
                "changeClosePercentage": 0.25,
                "previousClose": 100.25
              },
              "dateTime": 1640995200000
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(TEST_ACCOUNT_ID, "VTSAX");

    assertNotNull(quotes);
    assertFalse(quotes.isEmpty());
    assertEquals("VTSAX", quotes.get(0).get("symbol"));
    assertEquals(100.50, quotes.get(0).get("netAssetValue"));
    assertEquals("MF", quotes.get(0).get("securityType"));
  }

  @Test
  void getQuotes_withNullAccountId_usesDelayedQuotes() throws Exception {
    String mockResponse = """
        {
          "QuoteResponse": {
            "QuoteData": [{
              "Product": {"symbol": "AAPL", "companyName": "Apple Inc."},
              "All": {
                "lastTrade": 151.80,
                "previousClose": 150.00,
                "volume": 50000000
              },
              "dateTime": "2024-01-15T16:00:00-05:00"
            }]
          }
        }
        """;

    when(properties.getConsumerKey()).thenReturn("test_consumer_key");
    when(apiClient.makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull()))
        .thenReturn(mockResponse);

    List<Map<String, Object>> quotes = quoteClient.getQuotes(null, "AAPL");

    assertNotNull(quotes);
    assertFalse(quotes.isEmpty());
    assertEquals("AAPL", quotes.get(0).get("symbol"));
    
    // Verify non-OAuth request was used
    verify(apiClient, times(1)).makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull());
    verify(apiClient, never()).makeRequest(anyString(), anyString(), any(), any(), any());
  }
}
