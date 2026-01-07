package com.myqyl.aitradex.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.config.AlphaVantageProperties;
import com.myqyl.aitradex.exception.NotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Market data adapter for Alpha Vantage API.
 * Fetches real-time and daily stock quotes from Alpha Vantage.
 * 
 * @see <a href="https://www.alphavantage.co/documentation/">Alpha Vantage API Documentation</a>
 */
@Component
@ConditionalOnProperty(name = "app.alpha-vantage.enabled", havingValue = "true", matchIfMissing = true)
public class AlphaVantageAdapter implements MarketDataAdapter {

  private static final Logger log = LoggerFactory.getLogger(AlphaVantageAdapter.class);
  private static final String GLOBAL_QUOTE_FUNCTION = "GLOBAL_QUOTE";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final AlphaVantageProperties properties;

  @Autowired
  public AlphaVantageAdapter(ObjectMapper objectMapper, AlphaVantageProperties properties) {
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  // Constructor for testing with custom HttpClient
  AlphaVantageAdapter(HttpClient httpClient, ObjectMapper objectMapper, AlphaVantageProperties properties) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public String name() {
    return "alphavantage";
  }

  @Override
  public MarketDataQuoteDto latestQuote(String symbol) {
    String normalized = symbol.toUpperCase();
    String url = buildQuoteUrl(normalized);
    
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .header("Accept", "application/json")
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      
      if (response.statusCode() >= 300) {
        log.error("Alpha Vantage returned status {} for symbol {}", response.statusCode(), normalized);
        throw new IllegalStateException("Alpha Vantage returned " + response.statusCode());
      }

      return parseGlobalQuoteResponse(normalized, response.body());
    } catch (IOException ex) {
      log.error("Failed to fetch Alpha Vantage quote for {}", normalized, ex);
      throw new IllegalStateException("Failed to fetch Alpha Vantage quote", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Alpha Vantage request interrupted", ex);
    }
  }

  /**
   * Fetches intraday quote data for a symbol.
   * Uses TIME_SERIES_INTRADAY function with 1-minute intervals.
   */
  public MarketDataQuoteDto intradayQuote(String symbol) {
    String normalized = symbol.toUpperCase();
    String url = buildIntradayUrl(normalized);
    
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .header("Accept", "application/json")
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      
      if (response.statusCode() >= 300) {
        log.error("Alpha Vantage intraday returned status {} for symbol {}", response.statusCode(), normalized);
        throw new IllegalStateException("Alpha Vantage returned " + response.statusCode());
      }

      return parseIntradayResponse(normalized, response.body());
    } catch (IOException ex) {
      log.error("Failed to fetch Alpha Vantage intraday quote for {}", normalized, ex);
      throw new IllegalStateException("Failed to fetch Alpha Vantage intraday quote", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Alpha Vantage request interrupted", ex);
    }
  }

  private String buildQuoteUrl(String symbol) {
    return String.format("%s/query?function=%s&symbol=%s&apikey=%s",
        properties.getBaseUrl(),
        GLOBAL_QUOTE_FUNCTION,
        symbol,
        properties.getApiKey());
  }

  private String buildIntradayUrl(String symbol) {
    return String.format("%s/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=1min&apikey=%s",
        properties.getBaseUrl(),
        symbol,
        properties.getApiKey());
  }

  private MarketDataQuoteDto parseGlobalQuoteResponse(String symbol, String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      
      // Check for API error messages
      if (root.has("Error Message")) {
        throw new NotFoundException("Alpha Vantage quote for %s not found: %s"
            .formatted(symbol, root.get("Error Message").asText()));
      }
      
      // Check for rate limit message
      if (root.has("Note") || root.has("Information")) {
        String message = root.has("Note") ? root.get("Note").asText() : root.get("Information").asText();
        log.warn("Alpha Vantage API limit reached: {}", message);
        throw new IllegalStateException("Alpha Vantage API rate limit reached: " + message);
      }

      JsonNode globalQuote = root.path("Global Quote");
      if (globalQuote.isMissingNode() || globalQuote.isEmpty()) {
        throw new NotFoundException("Alpha Vantage quote for %s not found".formatted(symbol));
      }

      BigDecimal open = decimal(globalQuote, "02. open");
      BigDecimal high = decimal(globalQuote, "03. high");
      BigDecimal low = decimal(globalQuote, "04. low");
      BigDecimal close = decimal(globalQuote, "05. price");
      Long volume = longValue(globalQuote, "06. volume");
      String latestTradingDay = globalQuote.path("07. latest trading day").asText("");
      
      OffsetDateTime asOf = parseLatestTradingDay(latestTradingDay);

      return new MarketDataQuoteDto(symbol, asOf, open, high, low, close, volume, name());
    } catch (IOException ex) {
      log.error("Failed to parse Alpha Vantage response for {}", symbol, ex);
      throw new IllegalStateException("Failed to parse Alpha Vantage response", ex);
    }
  }

  private MarketDataQuoteDto parseIntradayResponse(String symbol, String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      
      // Check for API error messages
      if (root.has("Error Message")) {
        throw new NotFoundException("Alpha Vantage intraday quote for %s not found: %s"
            .formatted(symbol, root.get("Error Message").asText()));
      }
      
      // Check for rate limit message
      if (root.has("Note") || root.has("Information")) {
        String message = root.has("Note") ? root.get("Note").asText() : root.get("Information").asText();
        log.warn("Alpha Vantage API limit reached: {}", message);
        throw new IllegalStateException("Alpha Vantage API rate limit reached: " + message);
      }

      JsonNode timeSeries = root.path("Time Series (1min)");
      if (timeSeries.isMissingNode() || timeSeries.isEmpty()) {
        throw new NotFoundException("Alpha Vantage intraday quote for %s not found".formatted(symbol));
      }

      // Get the most recent entry (first key)
      Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
      if (!fields.hasNext()) {
        throw new NotFoundException("Alpha Vantage intraday quote for %s not found".formatted(symbol));
      }

      Map.Entry<String, JsonNode> latestEntry = fields.next();
      String timestamp = latestEntry.getKey();
      JsonNode data = latestEntry.getValue();

      BigDecimal open = decimal(data, "1. open");
      BigDecimal high = decimal(data, "2. high");
      BigDecimal low = decimal(data, "3. low");
      BigDecimal close = decimal(data, "4. close");
      Long volume = longValue(data, "5. volume");
      
      OffsetDateTime asOf = parseIntradayTimestamp(timestamp);

      return new MarketDataQuoteDto(symbol, asOf, open, high, low, close, volume, name());
    } catch (IOException ex) {
      log.error("Failed to parse Alpha Vantage intraday response for {}", symbol, ex);
      throw new IllegalStateException("Failed to parse Alpha Vantage intraday response", ex);
    }
  }

  private BigDecimal decimal(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      log.warn("Failed to parse decimal value '{}' for field '{}'", text, field);
      return null;
    }
  }

  private Long longValue(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException ex) {
      log.warn("Failed to parse long value '{}' for field '{}'", text, field);
      return null;
    }
  }

  private OffsetDateTime parseLatestTradingDay(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
    try {
      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      // Market close time is typically 4 PM ET, use 21:00 UTC as approximation
      return OffsetDateTime.of(date, LocalTime.of(21, 0), ZoneOffset.UTC);
    } catch (Exception ex) {
      log.warn("Failed to parse trading day '{}', using current time", dateStr);
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
  }

  private OffsetDateTime parseIntradayTimestamp(String timestamp) {
    if (timestamp == null || timestamp.isBlank()) {
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
    try {
      // Format: "2024-01-15 16:00:00"
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      return OffsetDateTime.of(
          java.time.LocalDateTime.parse(timestamp, formatter),
          ZoneOffset.of("-05:00") // Alpha Vantage uses US Eastern time
      ).withOffsetSameInstant(ZoneOffset.UTC);
    } catch (Exception ex) {
      log.warn("Failed to parse intraday timestamp '{}', using current time", timestamp);
      return OffsetDateTime.now(ZoneOffset.UTC);
    }
  }
}
