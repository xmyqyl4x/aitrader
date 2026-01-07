package com.myqyl.aitradex.marketdata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.exception.NotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YahooFinanceAdapterTest {

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private ObjectMapper objectMapper;
  private YahooFinanceAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    adapter = new YahooFinanceAdapter(objectMapper);
    // Use reflection to inject mock HttpClient
    Field httpClientField = YahooFinanceAdapter.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(adapter, httpClient);
  }

  @Test
  void name_returnsYahoo() {
    assertEquals("yahoo", adapter.name());
  }

  @Test
  void latestQuote_parsesQuoteResponse() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": [
              {
                "symbol": "AAPL",
                "regularMarketOpen": 150.25,
                "regularMarketDayHigh": 152.50,
                "regularMarketDayLow": 149.75,
                "regularMarketPrice": 151.80,
                "regularMarketVolume": 50000000,
                "regularMarketTime": 1705352400
              }
            ],
            "error": null
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
    assertEquals("152.5", quote.high().toPlainString());
    assertEquals("149.75", quote.low().toPlainString());
    assertEquals("151.8", quote.close().toPlainString());
    assertEquals(50000000L, quote.volume());
    assertEquals("yahoo", quote.source());
  }

  @Test
  void latestQuote_throwsNotFoundForEmptyResult() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": [],
            "error": null
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }

  @Test
  void latestQuote_throwsNotFoundForNullResult() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": null,
            "error": null
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }

  @Test
  void latestQuote_throwsIllegalStateForHttpError() throws Exception {
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    IllegalStateException ex = assertThrows(IllegalStateException.class, 
        () -> adapter.latestQuote("AAPL"));
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
          "quoteResponse": {
            "result": [
              {
                "symbol": "MSFT",
                "regularMarketOpen": 350.00,
                "regularMarketDayHigh": 355.00,
                "regularMarketDayLow": 348.00,
                "regularMarketPrice": 352.50,
                "regularMarketVolume": 25000000,
                "regularMarketTime": 1705352400
              }
            ],
            "error": null
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
  void latestQuote_handlesNullVolume() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": [
              {
                "symbol": "TEST",
                "regularMarketOpen": 100.00,
                "regularMarketDayHigh": 105.00,
                "regularMarketDayLow": 98.00,
                "regularMarketPrice": 102.00,
                "regularMarketTime": 1705352400
              }
            ],
            "error": null
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("TEST");

    assertNull(quote.volume());
  }

  @Test
  void latestQuote_handlesNullPriceFields() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": [
              {
                "symbol": "PARTIAL",
                "regularMarketPrice": 100.00,
                "regularMarketTime": 1705352400
              }
            ],
            "error": null
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("PARTIAL");

    assertNull(quote.open());
    assertNull(quote.high());
    assertNull(quote.low());
    assertEquals("100.0", quote.close().toPlainString());
  }

  @Test
  void latestQuote_handlesZeroTimestamp() throws Exception {
    String responseJson = """
        {
          "quoteResponse": {
            "result": [
              {
                "symbol": "TEST",
                "regularMarketPrice": 100.00,
                "regularMarketTime": 0
              }
            ],
            "error": null
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("TEST");

    assertNotNull(quote.asOf());
  }

  @Test
  void latestQuote_handlesMissingQuoteResponse() throws Exception {
    String responseJson = "{}";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }
}
