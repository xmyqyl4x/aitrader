package com.myqyl.aitradex.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.exception.NotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.market-data.providers.yahoo.enabled", havingValue = "true", matchIfMissing = false)
public class YahooFinanceAdapter implements MarketDataAdapter {

  private static final String ENDPOINT =
      "https://query1.finance.yahoo.com/v7/finance/quote?symbols=";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public YahooFinanceAdapter(ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = objectMapper;
  }

  @Override
  public String name() {
    return "yahoo";
  }

  @Override
  public MarketDataQuoteDto latestQuote(String symbol) {
    String normalized = symbol.toUpperCase();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(ENDPOINT + normalized)).GET().build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException("Yahoo Finance returned " + response.statusCode());
      }
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode result =
          root.path("quoteResponse").path("result").isArray()
              ? root.path("quoteResponse").path("result").path(0)
              : null;
      if (result == null || result.isMissingNode()) {
        throw new NotFoundException("Yahoo Finance quote for %s not found".formatted(symbol));
      }

      BigDecimal open = decimal(result, "regularMarketOpen");
      BigDecimal high = decimal(result, "regularMarketDayHigh");
      BigDecimal low = decimal(result, "regularMarketDayLow");
      BigDecimal close = decimal(result, "regularMarketPrice");
      Long volume =
          result.path("regularMarketVolume").isNumber()
              ? result.path("regularMarketVolume").longValue()
              : null;
      OffsetDateTime asOf = parseTime(result.path("regularMarketTime").asLong(0L));

      return new MarketDataQuoteDto(normalized, asOf, open, high, low, close, volume, name());
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to fetch Yahoo Finance quote", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Yahoo Finance request interrupted", ex);
    }
  }

  private BigDecimal decimal(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isNumber() ? value.decimalValue() : null;
  }

  private OffsetDateTime parseTime(long epochSeconds) {
    if (epochSeconds <= 0) {
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
  }
}
