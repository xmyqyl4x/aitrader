package com.myqyl.aitradex.marketdata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
class StooqAdapterTest {

  @Mock
  private HttpClient httpClient;

  @Mock
  private HttpResponse<String> httpResponse;

  private StooqAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    adapter = new StooqAdapter();
    // Use reflection to inject mock HttpClient
    Field httpClientField = StooqAdapter.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(adapter, httpClient);
  }

  @Test
  void name_returnsStooq() {
    assertEquals("stooq", adapter.name());
  }

  @Test
  void latestQuote_parsesCsvResponse() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        AAPL.US,2024-01-15,21:00:00,150.25,152.50,149.75,151.80,50000000
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
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
    assertEquals("stooq", quote.source());
  }

  @Test
  void latestQuote_handlesNoTimeInResponse() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        AAPL.US,2024-01-15,-,150.00,152.00,149.00,151.00,40000000
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("AAPL");

    assertNotNull(quote);
    assertEquals("AAPL", quote.symbol());
  }

  @Test
  void latestQuote_throwsNotFoundForNoData() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        No data
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("INVALID"));
  }

  @Test
  void latestQuote_throwsNotFoundForEmptyResponse() throws Exception {
    String csvResponse = "Symbol,Date,Time,Open,High,Low,Close,Volume";

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(NotFoundException.class, () -> adapter.latestQuote("EMPTY"));
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
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        MSFT.US,2024-01-15,21:00:00,350.00,355.00,348.00,352.50,25000000
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("msft");

    assertEquals("MSFT", quote.symbol());
  }

  @Test
  void latestQuote_handlesNullVolume() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        TEST.US,2024-01-15,21:00:00,100.00,105.00,98.00,102.00,-
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("TEST");

    assertNull(quote.volume());
  }

  @Test
  void latestQuote_handlesSymbolWithDot() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        TSLA.US,2024-01-15,21:00:00,240.00,245.00,238.00,242.50,30000000
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // Symbol already contains .US, should not add another .us
    MarketDataQuoteDto quote = adapter.latestQuote("TSLA.US");

    assertNotNull(quote);
  }

  @Test
  void latestQuote_appendsUsForSimpleSymbol() throws Exception {
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close,Volume
        GOOG.US,2024-01-15,21:00:00,140.00,142.00,139.00,141.50,20000000
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("GOOG");

    assertEquals("GOOG", quote.symbol());
  }

  @Test
  void latestQuote_handlesResponseWithoutVolume() throws Exception {
    // Some symbols don't have volume data
    String csvResponse = """
        Symbol,Date,Time,Open,High,Low,Close
        FOREX,2024-01-15,21:00:00,1.0850,1.0875,1.0825,1.0860
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(csvResponse);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    MarketDataQuoteDto quote = adapter.latestQuote("FOREX");

    assertNotNull(quote);
    assertNull(quote.volume());
  }
}
