package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import java.util.*;
import java.util.Arrays;
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
   * If accountId is null, uses delayed quotes with consumerKey (non-OAuth).
   * If accountId is provided, uses authenticated real-time quotes (OAuth).
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    try {
      // Build URL with symbols in path (matching example app: /v1/market/quote/{symbols})
      String symbolsPath = String.join(",", symbols);
      String url = properties.getQuoteUrl(symbolsPath);
      Map<String, String> params = new HashMap<>();
      
      String response;
      // Support delayed quotes for unauthenticated requests (matches example app behavior)
      if (accountId == null) {
        // Use delayed quotes with consumerKey query param (no OAuth)
        params.put("consumerKey", properties.getConsumerKey());
        response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
        log.debug("Using delayed quotes (non-OAuth) for symbols: {}", Arrays.toString(symbols));
      } else {
        // Use authenticated real-time quotes (OAuth)
        params.put("detailFlag", "ALL"); // Request all details for authenticated quotes
        response = apiClient.makeRequest("GET", url, params, null, accountId);
        log.debug("Using real-time quotes (OAuth) for symbols: {}", Arrays.toString(symbols));
      }
      
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
      quote.put("symbol", productNode.path("symbol").asText(""));
      quote.put("exchange", productNode.path("exchange").asText(""));
      quote.put("companyName", productNode.path("companyName").asText(""));
      quote.put("securityType", productNode.path("securityType").asText(""));
    }
    
    // DateTime
    JsonNode dateTimeNode = quoteNode.path("dateTime");
    if (!dateTimeNode.isMissingNode()) {
      if (dateTimeNode.isNumber()) {
        quote.put("dateTime", dateTimeNode.asLong());
      } else {
        quote.put("dateTime", dateTimeNode.asText(""));
      }
    }
    
    // All quotes (for stocks/ETFs)
    JsonNode allNode = quoteNode.path("All");
    if (!allNode.isMissingNode()) {
      quote.put("lastTrade", getDoubleValue(allNode, "lastTrade"));
      quote.put("previousClose", getDoubleValue(allNode, "previousClose"));
      quote.put("open", getDoubleValue(allNode, "open"));
      quote.put("high", getDoubleValue(allNode, "high"));
      quote.put("low", getDoubleValue(allNode, "low"));
      quote.put("totalVolume", getLongValue(allNode, "totalVolume"));
      quote.put("volume", getLongValue(allNode, "volume")); // Alias for totalVolume
      quote.put("changeClose", getDoubleValue(allNode, "changeClose"));
      quote.put("changeClosePercentage", getDoubleValue(allNode, "changeClosePercentage"));
      quote.put("change", getDoubleValue(allNode, "change"));
      quote.put("changePercent", getDoubleValue(allNode, "changePercent"));
      quote.put("bid", getDoubleValue(allNode, "bid"));
      quote.put("ask", getDoubleValue(allNode, "ask"));
      quote.put("bidSize", getIntValue(allNode, "bidSize"));
      quote.put("askSize", getIntValue(allNode, "askSize"));
      quote.put("timeOfLastTrade", getLongValue(allNode, "timeOfLastTrade"));
      quote.put("companyName", allNode.path("companyName").asText(""));
    }
    
    // MutualFund quotes (for mutual funds)
    JsonNode mutualFundNode = quoteNode.path("MutualFund");
    if (!mutualFundNode.isMissingNode()) {
      quote.put("netAssetValue", getDoubleValue(mutualFundNode, "netAssetValue"));
      quote.put("publicOfferPrice", getDoubleValue(mutualFundNode, "publicOfferPrice"));
      quote.put("changeClose", getDoubleValue(mutualFundNode, "changeClose"));
      quote.put("changeClosePercentage", getDoubleValue(mutualFundNode, "changeClosePercentage"));
      quote.put("previousClose", getDoubleValue(mutualFundNode, "previousClose"));
    }
    
    return quote;
  }

  private Double getDoubleValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asDouble();
    }
    try {
      return Double.parseDouble(fieldNode.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Long getLongValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asLong();
    }
    try {
      return Long.parseLong(fieldNode.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer getIntValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asInt();
    }
    try {
      return Integer.parseInt(fieldNode.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
