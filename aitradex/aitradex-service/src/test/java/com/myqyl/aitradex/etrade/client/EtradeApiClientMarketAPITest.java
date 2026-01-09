package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.market.dto.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeApiClientMarketAPI.
 * 
 * Tests all 4 Market API endpoints:
 * 1. Get Quotes
 * 2. Lookup Product
 * 3. Get Option Chains
 * 4. Get Option Expire Dates
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeApiClientMarketAPI Tests")
class EtradeApiClientMarketAPITest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeApiClientMarketAPI marketApi;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    marketApi = new EtradeApiClientMarketAPI(apiClient, properties, objectMapper);

    when(properties.getQuoteUrl(anyString()))
        .thenReturn("https://apisb.etrade.com/v1/market/quote/AAPL");
    when(properties.getLookupProductUrl())
        .thenReturn("https://apisb.etrade.com/v1/market/lookup");
    when(properties.getOptionChainsUrl())
        .thenReturn("https://apisb.etrade.com/v1/market/optionchains");
    when(properties.getOptionExpireDatesUrl())
        .thenReturn("https://apisb.etrade.com/v1/market/optionexpiredate");
    when(properties.getConsumerKey())
        .thenReturn("test_consumer_key");
  }

  @Test
  @DisplayName("getQuotes - Success with authenticated request")
  void getQuotes_shouldReturnQuotesList_authenticated() throws Exception {
    String responseJson = "{\"QuoteResponse\":{" +
        "\"QuoteData\":{" +
        "\"Product\":{\"symbol\":\"AAPL\",\"exchange\":\"NASDAQ\",\"securityType\":\"EQ\"}," +
        "\"dateTime\":1234567890000," +
        "\"All\":{" +
        "\"lastTrade\":150.0,\"previousClose\":148.5,\"open\":149.0," +
        "\"high\":151.0,\"low\":148.0,\"totalVolume\":1000000," +
        "\"changeClose\":1.5,\"changeClosePercentage\":1.01," +
        "\"bid\":149.9,\"ask\":150.1,\"bidSize\":1000,\"askSize\":1000," +
        "\"companyName\":\"Apple Inc.\"}}}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols("AAPL");
    request.setDetailFlag("ALL");

    QuoteResponse result = marketApi.getQuotes(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertNotNull(result.getQuoteData());
    assertEquals(1, result.getQuoteData().size());
    EtradeQuoteModel quote = result.getQuoteData().get(0);
    assertNotNull(quote.getAll());
    AllQuoteDetailsDto all = quote.getAll();
    assertNotNull(all.getProduct());
    assertEquals("AAPL", all.getProduct().getSymbol());
    assertEquals(150.0, all.getLastTrade());
    assertEquals(148.5, all.getPreviousClose());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("getQuotes - Success with delayed quotes (non-OAuth)")
  void getQuotes_shouldReturnQuotesList_delayed() throws Exception {
    String responseJson = "{\"QuoteResponse\":{" +
        "\"QuoteData\":{" +
        "\"Product\":{\"symbol\":\"AAPL\",\"exchange\":\"NASDAQ\",\"securityType\":\"EQ\"}," +
        "\"All\":{" +
        "\"lastTrade\":150.0,\"previousClose\":148.5," +
        "\"quoteType\":\"DELAYED\"}}}}}";

    when(apiClient.makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull()))
        .thenReturn(responseJson);

    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols("AAPL");

    QuoteResponse result = marketApi.getQuotes(null, request); // null accountId = delayed quotes

    assertNotNull(result);
    assertEquals(1, result.getQuoteData().size());
    assertNotNull(result.getQuoteData().get(0).getAll());
    assertEquals("DELAYED", result.getQuoteData().get(0).getAll().getQuoteType());

    verify(apiClient, times(1)).makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull());
    verify(apiClient, never()).makeRequest(anyString(), anyString(), any(), any(), any());
  }

  @Test
  @DisplayName("lookupProduct - Success")
  void lookupProduct_shouldReturnProductsList() throws Exception {
    String responseJson = "{\"LookupResponse\":{" +
        "\"Data\":[" +
        "{\"symbol\":\"AAPL\",\"description\":\"Apple Inc.\",\"type\":\"EQUITY\"}," +
        "{\"symbol\":\"AAPL-DE\",\"description\":\"Apple Inc.\",\"type\":\"EQUITY\"}]}}}";

    when(apiClient.makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull()))
        .thenReturn(responseJson);

    LookupProductRequest request = new LookupProductRequest();
    request.setInput("AAPL");

    LookupProductResponse result = marketApi.lookupProduct(request);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(2, result.getData().size());
    assertEquals("AAPL", result.getData().get(0).getSymbol());
    assertEquals("Apple Inc.", result.getData().get(0).getDescription());
    assertEquals("EQUITY", result.getData().get(0).getType());

    verify(apiClient, times(1)).makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull());
  }

  @Test
  @DisplayName("getOptionChains - Success")
  void getOptionChains_shouldReturnOptionChainResponse() throws Exception {
    String responseJson = "{\"OptionChainResponse\":{" +
        "\"symbol\":\"IBM\"," +
        "\"nearPrice\":200.0," +
        "\"adjustedFlag\":false," +
        "\"quoteType\":\"DELAYED\"," +
        "\"timestamp\":1529430484," +
        "\"SelectedED\":{\"year\":2018,\"month\":8,\"day\":17}," +
        "\"OptionPair\":[{" +
        "\"Call\":{" +
        "\"optionType\":\"CALL\",\"strikePrice\":200.0," +
        "\"bid\":0.0,\"ask\":0.05,\"volume\":0," +
        "\"OptionGreeks\":{\"delta\":0.0049,\"gamma\":0.0008,\"theta\":-0.002,\"vega\":0.0086,\"rho\":0.001,\"iv\":0.3145}}," +
        "\"Put\":{" +
        "\"optionType\":\"PUT\",\"strikePrice\":200.0," +
        "\"bid\":0.0,\"ask\":0.0}}}]}}";

    when(apiClient.makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull()))
        .thenReturn(responseJson);

    GetOptionChainsRequest request = new GetOptionChainsRequest();
    request.setSymbol("IBM");
    request.setExpiryYear(2018);
    request.setExpiryMonth(8);
    request.setExpiryDay(17);
    request.setStrikePriceNear(200);
    request.setNoOfStrikes(2);

    OptionChainResponse result = marketApi.getOptionChains(request);

    assertNotNull(result);
    assertEquals("IBM", result.getSymbol());
    assertEquals(200.0, result.getNearPrice());
    assertFalse(result.getAdjustedFlag());
    assertNotNull(result.getOptionPairs());
    assertEquals(1, result.getOptionPairs().size());
    OptionPairDto pair = result.getOptionPairs().get(0);
    assertNotNull(pair.getCall());
    assertEquals(200.0, pair.getCall().getStrikePrice());
    assertNotNull(pair.getCall().getOptionGreeks());
    assertEquals(0.0049, pair.getCall().getOptionGreeks().getDelta());

    verify(apiClient, times(1)).makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull());
  }

  @Test
  @DisplayName("getOptionExpireDates - Success")
  void getOptionExpireDates_shouldReturnExpireDatesList() throws Exception {
    String responseJson = "{\"OptionExpireDateResponse\":{" +
        "\"ExpireDate\":[" +
        "{\"year\":2018,\"month\":6,\"day\":22,\"expiryType\":\"WEEKLY\"}," +
        "{\"year\":2018,\"month\":6,\"day\":29,\"expiryType\":\"WEEKLY\"}," +
        "{\"year\":2018,\"month\":7,\"day\":20,\"expiryType\":\"MONTHLY\"}]}}";

    when(apiClient.makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull()))
        .thenReturn(responseJson);

    GetOptionExpireDatesRequest request = new GetOptionExpireDatesRequest();
    request.setSymbol("GOOG");
    request.setExpiryType("ALL");

    OptionExpireDateResponse result = marketApi.getOptionExpireDates(request);

    assertNotNull(result);
    assertNotNull(result.getExpireDates());
    assertEquals(3, result.getExpireDates().size());
    assertEquals(2018, result.getExpireDates().get(0).getYear());
    assertEquals(6, result.getExpireDates().get(0).getMonth());
    assertEquals(22, result.getExpireDates().get(0).getDay());
    assertEquals("WEEKLY", result.getExpireDates().get(0).getExpiryType());
    assertEquals("MONTHLY", result.getExpireDates().get(2).getExpiryType());

    verify(apiClient, times(1)).makeRequestWithoutOAuth(eq("GET"), anyString(), any(), isNull());
  }

  @Test
  @DisplayName("getQuotes - Error handling")
  void getQuotes_shouldHandleErrorResponse() throws Exception {
    String errorJson = "{\"Error\":{\"code\":\"1019\",\"message\":\"Invalid symbol\"}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorJson);

    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols("INVALID");

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        marketApi.getQuotes(TEST_ACCOUNT_ID, request));

    assertEquals(500, exception.getHttpStatus());
    assertEquals("GET_QUOTES_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("getQuotes - Handles empty quotes")
  void getQuotes_shouldHandleEmptyQuotes() throws Exception {
    String responseJson = "{\"QuoteResponse\":{\"QuoteData\":[]}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols("INVALID");

    QuoteResponse result = marketApi.getQuotes(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertNotNull(result.getQuoteData());
    assertEquals(0, result.getQuoteData().size());
  }
}
