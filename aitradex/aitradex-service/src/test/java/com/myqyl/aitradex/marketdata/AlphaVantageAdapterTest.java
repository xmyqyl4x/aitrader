package com.myqyl.aitradex.marketdata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.config.AlphaVantageProperties;
import com.myqyl.aitradex.exception.NotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlphaVantageAdapterTest {

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private ObjectMapper objectMapper;
  private AlphaVantageProperties properties;
  private AlphaVantageAdapter adapter;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    properties = new AlphaVantageProperties();
    properties.setApiKey("test-api-key");
    properties.setBaseUrl("https://www.alphavantage.co");
    adapter = new AlphaVantageAdapter(httpClient, objectMapper, properties);
  }

  @Test
  void name_returnsAlphavantage() {
    assertEquals("alphavantage", adapter.name());
  }

  @Test
  void latestQuote_parsesGlobalQuoteResponse() throws Exception {
    String responseJson = """
        {
          "Global Quote": {
            "01. symbol": "AAPL",
            "02. open": "150.25",
            "03. high": "152.50",
            "04. low": "149.75",
            "05. price": "151.80",
            "06. volume": "50000000",
            "07. latest trading day": "2024-01-15",
            "08. previous close": "150.00",
            "09. change": "1.80",
            "10. change percent": "1.20%"
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("AAPL");

    assertNotNull(quote);
    assertEquals("AAPL", quote.symbol());
    assertEquals("150.25", quote.open().toPlainString());
    assertEquals("152.50", quote.high().toPlainString());
    assertEquals("149.75", quote.low().toPlainString());
    assertEquals("151.80", quote.close().toPlainString());
    assertEquals(50000000L, quote.volume());
    assertEquals("alphavantage", quote.source());
  }

  @Test
  void latestQuote_throwsNotFoundForEmptyQuote() throws Exception {
    String responseJson = """
        {
          "Global Quote": {}
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }

  @Test
  void latestQuote_throwsNotFoundForErrorMessage() throws Exception {
    String responseJson = """
        {
          "Error Message": "Invalid API call. Please retry or visit the documentation."
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }

  @Test
  void latestQuote_throwsIllegalStateForRateLimit() throws Exception {
    String responseJson = """
        {
          "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute."
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adapter.latestQuote("AAPL"));
    assertTrue(ex.getMessage().contains("rate limit"));
  }

  @Test
  void latestQuote_throwsIllegalStateForHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> adapter.latestQuote("AAPL"));
    assertTrue(ex.getMessage().contains("500"));
  }

  @Test
  void latestQuote_throwsIllegalStateForIOException() throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Connection refused"));

    assertThrows(IllegalStateException.class, () -> adapter.latestQuote("AAPL"));
  }

  @Test
  void latestQuote_normalizesSymbolToUppercase() throws Exception {
    String responseJson = """
        {
          "Global Quote": {
            "01. symbol": "MSFT",
            "02. open": "350.00",
            "03. high": "355.00",
            "04. low": "348.00",
            "05. price": "352.50",
            "06. volume": "25000000",
            "07. latest trading day": "2024-01-15"
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("msft");

    assertEquals("MSFT", quote.symbol());
  }

  @Test
  void latestQuote_handlesNullValues() throws Exception {
    String responseJson = """
        {
          "Global Quote": {
            "01. symbol": "TEST",
            "05. price": "100.00",
            "07. latest trading day": "2024-01-15"
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("TEST");

    assertNotNull(quote);
    assertEquals("TEST", quote.symbol());
    assertNull(quote.open());
    assertNull(quote.high());
    assertNull(quote.low());
    assertEquals("100.00", quote.close().toPlainString());
    assertNull(quote.volume());
  }

  @Test
  void intradayQuote_parsesTimeSeriesResponse() throws Exception {
    String responseJson = """
        {
          "Meta Data": {
            "1. Information": "Intraday Prices",
            "2. Symbol": "AAPL"
          },
          "Time Series (1min)": {
            "2024-01-15 16:00:00": {
              "1. open": "151.50",
              "2. high": "151.75",
              "3. low": "151.25",
              "4. close": "151.60",
              "5. volume": "1500000"
            },
            "2024-01-15 15:59:00": {
              "1. open": "151.40",
              "2. high": "151.55",
              "3. low": "151.30",
              "4. close": "151.50",
              "5. volume": "1200000"
            }
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.intradayQuote("AAPL");

    assertNotNull(quote);
    assertEquals("AAPL", quote.symbol());
    assertEquals("151.50", quote.open().toPlainString());
    assertEquals("151.75", quote.high().toPlainString());
    assertEquals("151.25", quote.low().toPlainString());
    assertEquals("151.60", quote.close().toPlainString());
    assertEquals(1500000L, quote.volume());
  }

  @Test
  void intradayQuote_throwsNotFoundForEmptyTimeSeries() throws Exception {
    String responseJson = """
        {
          "Meta Data": {},
          "Time Series (1min)": {}
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.intradayQuote("INVALID"));
  }
}
