package com.myqyl.aitradex.marketdata;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.exception.NotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.market-data.providers.stooq.enabled", havingValue = "true", matchIfMissing = false)
public class StooqAdapter implements MarketDataAdapter {

  private static final String ENDPOINT = "https://stooq.com/q/l/?s=%s&i=d";

  private final HttpClient httpClient;

  public StooqAdapter() {
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public String name() {
    return "stooq";
  }

  @Override
  public MarketDataQuoteDto latestQuote(String symbol) {
    String stooqSymbol = toStooqSymbol(symbol);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(ENDPOINT.formatted(stooqSymbol))).GET().build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException("Stooq returned " + response.statusCode());
      }
      String[] lines = response.body().split("\\r?\\n");
      if (lines.length < 2) {
        throw new NotFoundException("Stooq quote for %s not found".formatted(symbol));
      }
      String[] values = lines[1].split(",");
      if (values.length < 7 || values[0].equalsIgnoreCase("No data")) {
        throw new NotFoundException("Stooq quote for %s not found".formatted(symbol));
      }
      String normalized = symbol.toUpperCase(Locale.ROOT);
      LocalDate date = LocalDate.parse(values[1]);
      LocalTime time = values[2].equals("-") ? LocalTime.MIDNIGHT : LocalTime.parse(values[2]);
      OffsetDateTime asOf =
          OffsetDateTime.of(LocalDateTime.of(date, time), ZoneOffset.UTC);
      BigDecimal open = decimal(values[3]);
      BigDecimal high = decimal(values[4]);
      BigDecimal low = decimal(values[5]);
      BigDecimal close = decimal(values[6]);
      Long volume = values.length > 7 ? longValue(values[7]) : null;

      return new MarketDataQuoteDto(normalized, asOf, open, high, low, close, volume, name());
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to fetch Stooq quote", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Stooq request interrupted", ex);
    }
  }

  private String toStooqSymbol(String symbol) {
    String normalized = symbol.toLowerCase(Locale.ROOT);
    if (normalized.contains(".")) {
      return normalized;
    }
    return normalized + ".us";
  }

  private BigDecimal decimal(String value) {
    if (value == null || value.isBlank() || value.equals("-")) {
      return null;
    }
    return new BigDecimal(value);
  }

  private Long longValue(String value) {
    if (value == null || value.isBlank() || value.equals("-")) {
      return null;
    }
    return Long.parseLong(value);
  }
}
