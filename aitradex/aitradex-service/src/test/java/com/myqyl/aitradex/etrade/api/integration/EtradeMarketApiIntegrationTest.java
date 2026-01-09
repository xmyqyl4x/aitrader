package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myqyl.aitradex.etrade.client.EtradeApiClientMarketAPI;
import com.myqyl.aitradex.etrade.market.dto.*;
import com.myqyl.aitradex.etrade.service.EtradeQuoteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE Market API endpoints.
 * 
 * These tests validate our Market REST API endpoints by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying Market API client
 * - Validating request/response handling, error handling, etc.
 */
@DisplayName("E*TRADE Market API Integration Tests")
class EtradeMarketApiIntegrationTest extends EtradeApiIntegrationTestBase {

  @MockBean
  private EtradeQuoteService quoteService;

  @MockBean
  private EtradeApiClientMarketAPI marketApi;

  @BeforeEach
  void setUpMarket() {
    // Additional setup for Market tests if needed
  }

  @Test
  @DisplayName("GET /api/etrade/quotes/{symbol} should return quote")
  void getQuote_shouldReturnQuote() throws Exception {
    UUID accountId = testAccountId;
    String symbol = "AAPL";

    EtradeQuoteModel quote = createTestQuote();

    when(quoteService.getQuote(eq(accountId), eq(symbol), anyString()))
        .thenReturn(quote);

    mockMvc.perform(get("/api/etrade/quotes/{symbol}", symbol)
            .param("accountId", accountId.toString())
            .param("detailFlag", "ALL"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.all").exists())
        .andExpect(jsonPath("$.all.product.symbol").value("AAPL"))
        .andExpect(jsonPath("$.all.lastTrade").value(150.0))
        .andExpect(jsonPath("$.all.previousClose").value(148.5));

    verify(quoteService, times(1)).getQuote(eq(accountId), eq(symbol), eq("ALL"));
  }

  @Test
  @DisplayName("GET /api/etrade/quotes should return quotes list")
  void getQuotes_shouldReturnQuotesList() throws Exception {
    UUID accountId = testAccountId;
    String symbols = "AAPL,GOOG";

    QuoteResponse response = createTestQuoteResponse();

    when(quoteService.getQuotes(eq(accountId), any(GetQuotesRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/quotes")
            .param("symbols", symbols)
            .param("accountId", accountId.toString())
            .param("detailFlag", "ALL"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.quoteData").isArray())
        .andExpect(jsonPath("$.quoteData[0].all.product.symbol").value("AAPL"))
        .andExpect(jsonPath("$.quoteData[0].all.lastTrade").value(150.0));

    verify(quoteService, times(1)).getQuotes(eq(accountId), any(GetQuotesRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/quotes/lookup should return products list")
  void lookupProduct_shouldReturnProductsList() throws Exception {
    String input = "AAPL";

    LookupProductResponse response = createTestLookupProductResponse();

    when(quoteService.lookupProduct(any(LookupProductRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/quotes/lookup")
            .param("input", input))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].symbol").value("AAPL"))
        .andExpect(jsonPath("$.data[0].description").value("Apple Inc."))
        .andExpect(jsonPath("$.data[0].type").value("EQUITY"));

    verify(quoteService, times(1)).lookupProduct(any(LookupProductRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/quotes/option-chains should return option chains")
  void getOptionChains_shouldReturnOptionChains() throws Exception {
    String symbol = "IBM";

    OptionChainResponse response = createTestOptionChainResponse();

    when(quoteService.getOptionChains(any(GetOptionChainsRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/quotes/option-chains")
            .param("symbol", symbol)
            .param("expiryYear", "2018")
            .param("expiryMonth", "8")
            .param("strikePriceNear", "200")
            .param("noOfStrikes", "2"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.symbol").value("IBM"))
        .andExpect(jsonPath("$.nearPrice").value(200.0))
        .andExpect(jsonPath("$.optionPairs").isArray())
        .andExpect(jsonPath("$.optionPairs[0].call.optionType").value("CALL"))
        .andExpect(jsonPath("$.optionPairs[0].call.strikePrice").value(200.0))
        .andExpect(jsonPath("$.optionPairs[0].call.optionGreeks.delta").value(0.0049));

    verify(quoteService, times(1)).getOptionChains(any(GetOptionChainsRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/quotes/option-expire-dates should return expire dates")
  void getOptionExpireDates_shouldReturnExpireDates() throws Exception {
    String symbol = "GOOG";

    OptionExpireDateResponse response = createTestOptionExpireDateResponse();

    when(quoteService.getOptionExpireDates(any(GetOptionExpireDatesRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/quotes/option-expire-dates")
            .param("symbol", symbol)
            .param("expiryType", "ALL"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.expireDates").isArray())
        .andExpect(jsonPath("$.expireDates[0].year").value(2018))
        .andExpect(jsonPath("$.expireDates[0].month").value(6))
        .andExpect(jsonPath("$.expireDates[0].day").value(22))
        .andExpect(jsonPath("$.expireDates[0].expiryType").value("WEEKLY"));

    verify(quoteService, times(1)).getOptionExpireDates(any(GetOptionExpireDatesRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/quotes/{symbol} should handle validation errors")
  void getQuote_shouldHandleValidationErrors() throws Exception {
    UUID accountId = testAccountId;
    String symbol = ""; // Invalid - empty symbol

    mockMvc.perform(get("/api/etrade/quotes/{symbol}", symbol)
            .param("accountId", accountId.toString()))
        .andExpect(status().isNotFound()); // Path variable empty may result in 404

    verify(quoteService, never()).getQuote(any(), any(), any());
  }

  // Helper methods

  private EtradeQuoteModel createTestQuote() {
    EtradeQuoteModel quote = new EtradeQuoteModel();

    AllQuoteDetailsDto all = new AllQuoteDetailsDto();

    QuoteProductDto product = new QuoteProductDto();
    product.setSymbol("AAPL");
    product.setExchange("NASDAQ");
    product.setCompanyName("Apple Inc.");
    product.setSecurityType("EQ");
    all.setProduct(product);

    all.setLastTrade(150.0);
    all.setPreviousClose(148.5);
    all.setOpen(149.0);
    all.setHigh(151.0);
    all.setLow(148.0);
    all.setTotalVolume(1000000L);
    all.setChangeClose(1.5);
    all.setChangeClosePercentage(1.01);
    all.setBid(149.9);
    all.setAsk(150.1);
    all.setBidSize(1000L);
    all.setAskSize(1000L);
    all.setDateTime(1234567890000L);
    all.setQuoteType("REALTIME");

    quote.setAll(all);

    return quote;
  }

  private QuoteResponse createTestQuoteResponse() {
    QuoteResponse response = new QuoteResponse();
    response.setQuoteData(List.of(createTestQuote()));
    return response;
  }

  private LookupProductResponse createTestLookupProductResponse() {
    LookupProductResponse response = new LookupProductResponse();

    LookupProductDto product1 = new LookupProductDto();
    product1.setSymbol("AAPL");
    product1.setDescription("Apple Inc.");
    product1.setType("EQUITY");

    LookupProductDto product2 = new LookupProductDto();
    product2.setSymbol("AAPL-DE");
    product2.setDescription("Apple Inc.");
    product2.setType("EQUITY");

    response.setData(List.of(product1, product2));

    return response;
  }

  private OptionChainResponse createTestOptionChainResponse() {
    OptionChainResponse response = new OptionChainResponse();
    response.setSymbol("IBM");
    response.setNearPrice(200.0);
    response.setAdjustedFlag(false);
    response.setQuoteType("DELAYED");
    response.setTimestamp(1529430484L);

    SelectedEDDto selectedED = new SelectedEDDto();
    selectedED.setYear(2018);
    selectedED.setMonth(8);
    selectedED.setDay(17);
    response.setSelectedED(selectedED);

    OptionPairDto pair = new OptionPairDto();
    pair.setStrikePrice(200.0);

    OptionDto call = new OptionDto();
    call.setOptionType("CALL");
    call.setStrikePrice(200.0);
    call.setBid(0.0);
    call.setAsk(0.05);
    call.setVolume(0L);

    OptionGreeksDto greeks = new OptionGreeksDto();
    greeks.setDelta(0.0049);
    greeks.setGamma(0.0008);
    greeks.setTheta(-0.002);
    greeks.setVega(0.0086);
    greeks.setRho(0.001);
    greeks.setIv(0.3145);
    greeks.setCurrentValue(true);
    call.setOptionGreeks(greeks);

    OptionDto put = new OptionDto();
    put.setOptionType("PUT");
    put.setStrikePrice(200.0);
    put.setBid(0.0);
    put.setAsk(0.0);

    pair.setCall(call);
    pair.setPut(put);

    response.setOptionPairs(List.of(pair));

    return response;
  }

  private OptionExpireDateResponse createTestOptionExpireDateResponse() {
    OptionExpireDateResponse response = new OptionExpireDateResponse();

    OptionExpireDateDto date1 = new OptionExpireDateDto();
    date1.setYear(2018);
    date1.setMonth(6);
    date1.setDay(22);
    date1.setExpiryType("WEEKLY");

    OptionExpireDateDto date2 = new OptionExpireDateDto();
    date2.setYear(2018);
    date2.setMonth(7);
    date2.setDay(20);
    date2.setExpiryType("MONTHLY");

    response.setExpireDates(List.of(date1, date2));

    return response;
  }
}
