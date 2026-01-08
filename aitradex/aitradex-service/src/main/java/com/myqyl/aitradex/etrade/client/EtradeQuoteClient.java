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
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param symbols One or more stock symbols
   * @param detailFlag Detail flag (e.g., "ALL", "FUNDAMENTAL", "INTRADAY") (optional, default: "ALL" for authenticated)
   * @param requireEarningsDate Whether to include earnings date (optional)
   * @param overrideSymbolCount Override symbol count limit (optional)
   * @param skipMiniOptionsCheck Skip mini options check (optional)
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String[] symbols, String detailFlag,
                                             Boolean requireEarningsDate, Integer overrideSymbolCount,
                                             Boolean skipMiniOptionsCheck) {
    try {
      // Build URL with symbols in path (matching example app: /v1/market/quote/{symbols})
      String symbolsPath = String.join(",", symbols);
      String url = properties.getQuoteUrl(symbolsPath);
      Map<String, String> params = new HashMap<>();
      
      // Add optional parameters (apply to both authenticated and unauthenticated)
      if (requireEarningsDate != null) {
        params.put("requireEarningsDate", String.valueOf(requireEarningsDate));
      }
      if (overrideSymbolCount != null && overrideSymbolCount > 0) {
        params.put("overrideSymbolCount", String.valueOf(overrideSymbolCount));
      }
      if (skipMiniOptionsCheck != null) {
        params.put("skipMiniOptionsCheck", String.valueOf(skipMiniOptionsCheck));
      }
      
      String response;
      // Support delayed quotes for unauthenticated requests (matches example app behavior)
      if (accountId == null) {
        // Use delayed quotes with consumerKey query param (no OAuth)
        params.put("consumerKey", properties.getConsumerKey());
        response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
        log.debug("Using delayed quotes (non-OAuth) for symbols: {}", Arrays.toString(symbols));
      } else {
        // Use authenticated real-time quotes (OAuth)
        if (detailFlag != null && !detailFlag.isEmpty()) {
          params.put("detailFlag", detailFlag);
        } else {
          params.put("detailFlag", "ALL"); // Default to ALL for authenticated quotes
        }
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

  /**
   * Gets quote for one or more symbols (simplified version with defaults).
   * If accountId is null, uses delayed quotes with consumerKey (non-OAuth).
   * If accountId is provided, uses authenticated real-time quotes (OAuth).
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    return getQuotes(accountId, symbols, null, null, null, null);
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

  /**
   * Looks up products by symbol or company name.
   */
  public List<Map<String, Object>> lookupProduct(String input) {
    try {
      String url = properties.getLookupProductUrl();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      params.put("input", input);
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode dataNode = root.path("LookupResponse").path("Data");
      
      List<Map<String, Object>> products = new ArrayList<>();
      if (dataNode.isArray()) {
        for (JsonNode productNode : dataNode) {
          products.add(parseProductLookup(productNode));
        }
      } else if (dataNode.isObject()) {
        products.add(parseProductLookup(dataNode));
      }
      
      return products;
    } catch (Exception e) {
      log.error("Failed to lookup product for input {}", input, e);
      throw new RuntimeException("Failed to lookup product", e);
    }
  }

  /**
   * Gets option chains for a symbol.
   */
  public Map<String, Object> getOptionChains(String symbol, Integer expiryYear, Integer expiryMonth,
      Integer expiryDay, Integer strikePriceNear, Integer noOfStrikes, String optionCategory, String chainType) {
    try {
      String url = properties.getOptionChainsUrl();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      params.put("symbol", symbol);
      if (expiryYear != null) {
        params.put("expiryYear", String.valueOf(expiryYear));
      }
      if (expiryMonth != null) {
        params.put("expiryMonth", String.valueOf(expiryMonth));
      }
      if (expiryDay != null) {
        params.put("expiryDay", String.valueOf(expiryDay));
      }
      if (strikePriceNear != null) {
        params.put("strikePriceNear", String.valueOf(strikePriceNear));
      }
      if (noOfStrikes != null) {
        params.put("noOfStrikes", String.valueOf(noOfStrikes));
      }
      if (optionCategory != null && !optionCategory.isEmpty()) {
        params.put("optionCategory", optionCategory);
      }
      if (chainType != null && !chainType.isEmpty()) {
        params.put("chainType", chainType);
      }
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode optionChainNode = root.path("OptionChainResponse");
      
      return parseOptionChain(optionChainNode);
    } catch (Exception e) {
      log.error("Failed to get option chains for symbol {}", symbol, e);
      throw new RuntimeException("Failed to get option chains", e);
    }
  }

  /**
   * Gets option expire dates for a symbol.
   * 
   * @param symbol Stock symbol
   * @param expiryType Expiry type filter (e.g., "WEEKLY", "MONTHLY") (optional)
   */
  public List<Map<String, Object>> getOptionExpireDates(String symbol, String expiryType) {
    try {
      String url = properties.getOptionExpireDatesUrl();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      params.put("symbol", symbol);
      
      if (expiryType != null && !expiryType.isEmpty()) {
        params.put("expiryType", expiryType);
      }
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode expireDateNode = root.path("OptionExpireDateResponse").path("ExpireDate");
      
      List<Map<String, Object>> expireDates = new ArrayList<>();
      if (expireDateNode.isArray()) {
        for (JsonNode dateNode : expireDateNode) {
          expireDates.add(parseExpireDate(dateNode));
        }
      } else if (expireDateNode.isObject()) {
        expireDates.add(parseExpireDate(expireDateNode));
      }
      
      return expireDates;
    } catch (Exception e) {
      log.error("Failed to get option expire dates for symbol {}", symbol, e);
      throw new RuntimeException("Failed to get option expire dates", e);
    }
  }

  /**
   * Gets option expire dates for a symbol (simplified version).
   */
  public List<Map<String, Object>> getOptionExpireDates(String symbol) {
    return getOptionExpireDates(symbol, null);
  }

  private Map<String, Object> parseProductLookup(JsonNode productNode) {
    Map<String, Object> product = new HashMap<>();
    product.put("symbol", productNode.path("symbol").asText(""));
    product.put("description", productNode.path("description").asText(""));
    product.put("securityType", productNode.path("securityType").asText(""));
    return product;
  }

  private Map<String, Object> parseOptionChain(JsonNode optionChainNode) {
    Map<String, Object> chain = new HashMap<>();
    
    // Option chain metadata
    chain.put("symbol", optionChainNode.path("symbol").asText(""));
    chain.put("nearPrice", getDoubleValue(optionChainNode, "nearPrice"));
    chain.put("adjustedFlag", optionChainNode.path("adjustedFlag").asBoolean(false));
    chain.put("optionChainType", optionChainNode.path("optionChainType").asText(""));
    
    // Option pair data
    JsonNode optionPairNode = optionChainNode.path("OptionPair");
    if (!optionPairNode.isMissingNode()) {
      List<Map<String, Object>> pairs = new ArrayList<>();
      if (optionPairNode.isArray()) {
        for (JsonNode pairNode : optionPairNode) {
          pairs.add(parseOptionPair(pairNode));
        }
      } else {
        pairs.add(parseOptionPair(optionPairNode));
      }
      chain.put("optionPairs", pairs);
    }
    
    return chain;
  }

  private Map<String, Object> parseOptionPair(JsonNode pairNode) {
    Map<String, Object> pair = new HashMap<>();
    
    // Strike price (common to both call and put)
    pair.put("strikePrice", getDoubleValue(pairNode, "strikePrice"));
    
    JsonNode callNode = pairNode.path("Call");
    if (!callNode.isMissingNode()) {
      pair.put("call", parseOption(callNode));
    }
    
    JsonNode putNode = pairNode.path("Put");
    if (!putNode.isMissingNode()) {
      pair.put("put", parseOption(putNode));
    }
    
    return pair;
  }

  private Map<String, Object> parseOption(JsonNode optionNode) {
    Map<String, Object> option = new HashMap<>();
    option.put("strikePrice", getDoubleValue(optionNode, "strikePrice"));
    option.put("bid", getDoubleValue(optionNode, "bid"));
    option.put("ask", getDoubleValue(optionNode, "ask"));
    option.put("lastPrice", getDoubleValue(optionNode, "lastPrice"));
    option.put("volume", getLongValue(optionNode, "volume"));
    return option;
  }

  private Map<String, Object> parseExpireDate(JsonNode dateNode) {
    Map<String, Object> date = new HashMap<>();
    date.put("year", dateNode.path("year").asInt(0));
    date.put("month", dateNode.path("month").asInt(0));
    date.put("day", dateNode.path("day").asInt(0));
    return date;
  }
}
