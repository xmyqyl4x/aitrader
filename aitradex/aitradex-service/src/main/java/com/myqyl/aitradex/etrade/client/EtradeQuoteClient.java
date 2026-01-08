package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import java.util.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for E*TRADE Quote API endpoints.
 */
@Component
public class EtradeQuoteClient {

  private static final Logger log = LoggerFactory.getLogger(EtradeQuoteClient.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeQuoteClient(EtradeApiClient apiClient, EtradeProperties properties, 
                          ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * Gets quote for one or more symbols.
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    try {
      String url = properties.getQuoteUrl();
      Map<String, String> params = new HashMap<>();
      params.put("symbol", String.join(",", symbols));
      params.put("detailFlag", "ALL");
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode quotesNode = root.path("QuoteResponse").path("QuoteData");
      
      List<Map<String, Object>> quotes = new ArrayList<>();
      if (quotesNode.isArray()) {
        for (JsonNode quoteNode : quotesNode) {
          quotes.add(parseQuote(quoteNode));
        }
      } else if (quotesNode.isObject()) {
        quotes.add(parseQuote(quotesNode));
      }
      
      return quotes;
    } catch (Exception e) {
      log.error("Failed to get quotes for symbols {}", Arrays.toString(symbols), e);
      throw new RuntimeException("Failed to get quotes", e);
    }
  }

  private Map<String, Object> parseQuote(JsonNode quoteNode) {
    Map<String, Object> quote = new HashMap<>();
    
    // Product information
    JsonNode productNode = quoteNode.path("Product");
    if (!productNode.isMissingNode()) {
      quote.put("symbol", productNode.path("symbol").asText());
      quote.put("exchange", productNode.path("exchange").asText());
      quote.put("companyName", productNode.path("companyName").asText());
    }
    
    // All quotes
    JsonNode allNode = quoteNode.path("All");
    if (!allNode.isMissingNode()) {
      quote.put("lastTrade", allNode.path("lastTrade").asDouble());
      quote.put("previousClose", allNode.path("previousClose").asDouble());
      quote.put("open", allNode.path("open").asDouble());
      quote.put("high", allNode.path("high").asDouble());
      quote.put("low", allNode.path("low").asDouble());
      quote.put("volume", allNode.path("volume").asLong());
      quote.put("change", allNode.path("change").asDouble());
      quote.put("changePercent", allNode.path("changePercent").asDouble());
      quote.put("bid", allNode.path("bid").asDouble());
      quote.put("ask", allNode.path("ask").asDouble());
      quote.put("bidSize", allNode.path("bidSize").asInt());
      quote.put("askSize", allNode.path("askSize").asInt());
      quote.put("timeOfLastTrade", allNode.path("timeOfLastTrade").asLong());
    }
    
    return quote;
  }
}
