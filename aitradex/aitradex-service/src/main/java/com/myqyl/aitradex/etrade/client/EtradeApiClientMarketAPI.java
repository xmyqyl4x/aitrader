package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.market.dto.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E*TRADE Market API Client.
 * 
 * This class refactors market/quote-specific functionality from EtradeQuoteClient
 * into a dedicated Market API layer.
 * 
 * Implements all 4 Market API endpoints as per E*TRADE Market API documentation:
 * 1. Get Quotes
 * 2. Lookup Product
 * 3. Get Option Chains
 * 4. Get Option Expire Dates
 * 
 * All request and response objects are DTOs/Models, not Maps, as per requirements.
 */
@Component
public class EtradeApiClientMarketAPI {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClientMarketAPI.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeApiClientMarketAPI(
      EtradeApiClient apiClient,
      EtradeProperties properties,
      ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * 1. Get Quotes
   * 
   * Returns detailed quote information for one or more specified securities.
   * Supports delayed quotes (non-OAuth) and real-time quotes (OAuth).
   * 
   * @param accountId Internal account UUID for authentication (null for delayed quotes)
   * @param request GetQuotesRequest DTO containing symbols and query parameters
   * @return QuoteResponse DTO containing list of quotes
   * @throws EtradeApiException if the request fails
   */
  public QuoteResponse getQuotes(UUID accountId, GetQuotesRequest request) {
    try {
      // Build URL with symbols in path
      String symbolsPath = request.getSymbols();
      String url = properties.getQuoteUrl(symbolsPath);
      Map<String, String> params = new HashMap<>();
      
      // Add optional parameters (apply to both authenticated and unauthenticated)
      if (request.getRequireEarningsDate() != null) {
        params.put("requireEarningsDate", String.valueOf(request.getRequireEarningsDate()));
      }
      if (Boolean.TRUE.equals(request.getOverrideSymbolCount())) {
        params.put("overrideSymbolCount", "true");
      }
      if (request.getSkipMiniOptionsCheck() != null) {
        params.put("skipMiniOptionsCheck", String.valueOf(request.getSkipMiniOptionsCheck()));
      }
      
      String response;
      // Support delayed quotes for unauthenticated requests
      if (accountId == null) {
        // Use delayed quotes with consumerKey query param (no OAuth)
        params.put("consumerKey", properties.getConsumerKey());
        response = apiClient.makeRequestWithoutOAuth("GET", url, params.isEmpty() ? null : params, null);
        log.debug("Using delayed quotes (non-OAuth) for symbols: {}", request.getSymbols());
      } else {
        // Use authenticated real-time quotes (OAuth)
        if (request.getDetailFlag() != null && !request.getDetailFlag().isEmpty()) {
          params.put("detailFlag", request.getDetailFlag());
        } else {
          params.put("detailFlag", "ALL"); // Default to ALL for authenticated quotes
        }
        response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
        log.debug("Using real-time quotes (OAuth) for symbols: {}", request.getSymbols());
      }
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode quoteResponseNode = root.path("QuoteResponse");
      
      return parseQuoteResponse(quoteResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get quotes for symbols {}", request.getSymbols(), e);
      throw new EtradeApiException(500, "GET_QUOTES_FAILED", 
          "Failed to get quotes: " + e.getMessage(), e);
    }
  }

  /**
   * 2. Lookup Product
   * 
   * Searches for products by symbol or company name.
   * 
   * @param request LookupProductRequest DTO containing search input
   * @return LookupProductResponse DTO containing list of matching products
   * @throws EtradeApiException if the request fails
   */
  public LookupProductResponse lookupProduct(LookupProductRequest request) {
    try {
      String url = properties.getLookupProductUrl() + "/" + request.getInput();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode lookupResponseNode = root.path("LookupResponse");
      
      return parseLookupProductResponse(lookupResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to lookup product for input {}", request.getInput(), e);
      throw new EtradeApiException(500, "LOOKUP_PRODUCT_FAILED", 
          "Failed to lookup product: " + e.getMessage(), e);
    }
  }

  /**
   * 3. Get Option Chains
   * 
   * Returns option chains for a symbol.
   * 
   * @param request GetOptionChainsRequest DTO containing symbol and option chain parameters
   * @return OptionChainResponse DTO containing option chains
   * @throws EtradeApiException if the request fails
   */
  public OptionChainResponse getOptionChains(GetOptionChainsRequest request) {
    try {
      String url = properties.getOptionChainsUrl();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      params.put("symbol", request.getSymbol());
      
      if (request.getExpiryYear() != null) {
        params.put("expiryYear", String.valueOf(request.getExpiryYear()));
      }
      if (request.getExpiryMonth() != null) {
        params.put("expiryMonth", String.valueOf(request.getExpiryMonth()));
      }
      if (request.getExpiryDay() != null) {
        params.put("expiryDay", String.valueOf(request.getExpiryDay()));
      }
      if (request.getStrikePriceNear() != null) {
        params.put("strikePriceNear", String.valueOf(request.getStrikePriceNear()));
      }
      if (request.getNoOfStrikes() != null) {
        params.put("noOfStrikes", String.valueOf(request.getNoOfStrikes()));
      }
      if (request.getOptionCategory() != null && !request.getOptionCategory().isEmpty()) {
        params.put("optionCategory", request.getOptionCategory());
      }
      if (request.getChainType() != null && !request.getChainType().isEmpty()) {
        params.put("chainType", request.getChainType());
      }
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode optionChainNode = root.path("OptionChainResponse");
      
      return parseOptionChainResponse(optionChainNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get option chains for symbol {}", request.getSymbol(), e);
      throw new EtradeApiException(500, "GET_OPTION_CHAINS_FAILED", 
          "Failed to get option chains: " + e.getMessage(), e);
    }
  }

  /**
   * 4. Get Option Expire Dates
   * 
   * Returns expiration dates for options on a symbol.
   * 
   * @param request GetOptionExpireDatesRequest DTO containing symbol and expiry type filter
   * @return OptionExpireDateResponse DTO containing list of expiration dates
   * @throws EtradeApiException if the request fails
   */
  public OptionExpireDateResponse getOptionExpireDates(GetOptionExpireDatesRequest request) {
    try {
      String url = properties.getOptionExpireDatesUrl();
      Map<String, String> params = new HashMap<>();
      params.put("consumerKey", properties.getConsumerKey());
      params.put("symbol", request.getSymbol());
      
      if (request.getExpiryType() != null && !request.getExpiryType().isEmpty()) {
        params.put("expiryType", request.getExpiryType());
      }
      
      String response = apiClient.makeRequestWithoutOAuth("GET", url, params, null);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode expireDateResponseNode = root.path("OptionExpireDateResponse");
      
      return parseOptionExpireDateResponse(expireDateResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get option expire dates for symbol {}", request.getSymbol(), e);
      throw new EtradeApiException(500, "GET_OPTION_EXPIRE_DATES_FAILED", 
          "Failed to get option expire dates: " + e.getMessage(), e);
    }
  }

  // ============================================================================
  // Response Parsers (convert JSON to DTOs)
  // ============================================================================

  /**
   * Parses QuoteResponse JSON node into QuoteResponse DTO.
   */
  private QuoteResponse parseQuoteResponse(JsonNode quoteResponseNode) {
    QuoteResponse response = new QuoteResponse();
    
    // Parse QuoteData array
    JsonNode quoteDataNode = quoteResponseNode.path("QuoteData");
    List<EtradeQuoteModel> quotes = new ArrayList<>();
    if (quoteDataNode.isArray()) {
      for (JsonNode quoteNode : quoteDataNode) {
        quotes.add(parseQuote(quoteNode));
      }
    } else if (quoteDataNode.isObject() && !quoteDataNode.isMissingNode()) {
      quotes.add(parseQuote(quoteDataNode));
    }
    response.setQuoteData(quotes);
    
    return response;
  }

  /**
   * Parses Quote JSON node into EtradeQuoteModel DTO.
   * Handles different detailFlag responses (ALL, FUNDAMENTAL, INTRADAY, OPTIONS, WEEK_52, MF_DETAIL).
   * 
   * Structure: Product and DateTime are at quoteNode level, All/Fundamental/etc. are separate sections.
   */
  private EtradeQuoteModel parseQuote(JsonNode quoteNode) {
    EtradeQuoteModel quote = new EtradeQuoteModel();
    
    // Parse All quote details (for ALL detailFlag)
    // Product and DateTime are at quoteNode level, All contains the detailed quote data
    JsonNode allNode = quoteNode.path("All");
    if (!allNode.isMissingNode()) {
      AllQuoteDetailsDto allDetails = parseAllQuoteDetails(allNode);
      // Merge Product and DateTime from quoteNode level into All details
      mergeQuoteLevelFields(quoteNode, allDetails);
      quote.setAll(allDetails);
    }
    
    // Parse Fundamental quote details (for FUNDAMENTAL detailFlag)
    JsonNode fundamentalNode = quoteNode.path("Fundamental");
    if (!fundamentalNode.isMissingNode()) {
      AllQuoteDetailsDto fundamentalDetails = parseAllQuoteDetails(fundamentalNode);
      mergeQuoteLevelFields(quoteNode, fundamentalDetails);
      quote.setFundamental(fundamentalDetails);
    }
    
    // Parse Intraday quote details (for INTRADAY detailFlag)
    JsonNode intradayNode = quoteNode.path("Intraday");
    if (!intradayNode.isMissingNode()) {
      AllQuoteDetailsDto intradayDetails = parseAllQuoteDetails(intradayNode);
      mergeQuoteLevelFields(quoteNode, intradayDetails);
      quote.setIntraday(intradayDetails);
    }
    
    // Parse Options quote details (for OPTIONS detailFlag)
    JsonNode optionsNode = quoteNode.path("Options");
    if (!optionsNode.isMissingNode()) {
      AllQuoteDetailsDto optionsDetails = parseAllQuoteDetails(optionsNode);
      mergeQuoteLevelFields(quoteNode, optionsDetails);
      quote.setOptions(optionsDetails);
    }
    
    // Parse Week52 quote details (for WEEK_52 detailFlag)
    JsonNode week52Node = quoteNode.path("Week52");
    if (!week52Node.isMissingNode()) {
      AllQuoteDetailsDto week52Details = parseAllQuoteDetails(week52Node);
      mergeQuoteLevelFields(quoteNode, week52Details);
      quote.setWeek52(week52Details);
    }
    
    // Parse MutualFund quote details (for MF_DETAIL detailFlag)
    JsonNode mutualFundNode = quoteNode.path("MutualFund");
    if (!mutualFundNode.isMissingNode()) {
      AllQuoteDetailsDto mutualFundDetails = parseAllQuoteDetails(mutualFundNode);
      mergeQuoteLevelFields(quoteNode, mutualFundDetails);
      quote.setMutualFund(mutualFundDetails);
    }
    
    // If no specific detailFlag section, parse from root with Product and DateTime
    if (quote.getAll() == null && quote.getIntraday() == null && quote.getFundamental() == null
        && quote.getOptions() == null && quote.getWeek52() == null && quote.getMutualFund() == null) {
      // Try parsing from root node (some responses may not have All/Fundamental/etc. wrapper)
      AllQuoteDetailsDto allDetails = parseAllQuoteDetails(quoteNode);
      if (allDetails != null) {
        mergeQuoteLevelFields(quoteNode, allDetails);
        quote.setAll(allDetails);
      }
    }
    
    return quote;
  }

  /**
   * Merges Product and DateTime from quoteNode level into AllQuoteDetailsDto.
   * These fields are at the quoteNode level, not inside All/Fundamental/etc.
   */
  private void mergeQuoteLevelFields(JsonNode quoteNode, AllQuoteDetailsDto details) {
    if (details == null) {
      return;
    }
    
    // Parse Product from quoteNode level (if not already in details)
    if (details.getProduct() == null) {
      JsonNode productNode = quoteNode.path("Product");
      if (!productNode.isMissingNode()) {
        QuoteProductDto product = new QuoteProductDto();
        product.setSymbol(getStringValue(productNode, "symbol"));
        product.setExchange(getStringValue(productNode, "exchange"));
        product.setCompanyName(getStringValue(productNode, "companyName"));
        product.setSecurityType(getStringValue(productNode, "securityType"));
        details.setProduct(product);
      }
    }
    
    // Parse DateTime from quoteNode level (if not already in details)
    if (details.getDateTime() == null) {
      JsonNode dateTimeNode = quoteNode.path("dateTime");
      if (!dateTimeNode.isMissingNode()) {
        if (dateTimeNode.isNumber()) {
          details.setDateTime(dateTimeNode.asLong());
        } else {
          String dateTimeText = dateTimeNode.asText();
          if (dateTimeText != null && !dateTimeText.isEmpty()) {
            try {
              details.setDateTime(Long.parseLong(dateTimeText));
            } catch (NumberFormatException e) {
              // Ignore parsing errors
            }
          }
        }
      }
    }
  }

  /**
   * Parses AllQuoteDetails JSON node into AllQuoteDetailsDto DTO.
   * This is a comprehensive parser that handles all fields from the ALL detailFlag.
   * 
   * Note: Product and DateTime are typically at the quoteNode level, not inside All.
   * They will be merged by mergeQuoteLevelFields if needed.
   */
  private AllQuoteDetailsDto parseAllQuoteDetails(JsonNode allNode) {
    if (allNode.isMissingNode() || allNode.isNull()) {
      return null;
    }
    
    AllQuoteDetailsDto details = new AllQuoteDetailsDto();
    
    // Note: Product and DateTime are typically at quoteNode level, not here
    // They will be merged by mergeQuoteLevelFields if needed
    // But check here too in case they're in the detail section
    JsonNode productNode = allNode.path("Product");
    if (!productNode.isMissingNode()) {
      QuoteProductDto product = new QuoteProductDto();
      product.setSymbol(getStringValue(productNode, "symbol"));
      product.setExchange(getStringValue(productNode, "exchange"));
      product.setCompanyName(getStringValue(productNode, "companyName"));
      product.setSecurityType(getStringValue(productNode, "securityType"));
      details.setProduct(product);
    }
    
    // DateTime might be at detail level too
    details.setDateTime(getLongValue(allNode, "dateTime"));
    
    // Parse price information
    details.setLastTrade(getDoubleValue(allNode, "lastTrade"));
    details.setPreviousClose(getDoubleValue(allNode, "previousClose"));
    details.setOpen(getDoubleValue(allNode, "open"));
    details.setHigh(getDoubleValue(allNode, "high"));
    details.setLow(getDoubleValue(allNode, "low"));
    details.setHigh52(getDoubleValue(allNode, "high52"));
    details.setLow52(getDoubleValue(allNode, "low52"));
    
    // Parse volume information
    details.setTotalVolume(getLongValue(allNode, "totalVolume"));
    details.setVolume(getLongValue(allNode, "volume"));
    
    // Parse change information
    details.setChangeClose(getDoubleValue(allNode, "changeClose"));
    details.setChangeClosePercentage(getDoubleValue(allNode, "changeClosePercentage"));
    details.setDirLast(getStringValue(allNode, "dirLast"));
    
    // Parse bid/ask information
    details.setBid(getDoubleValue(allNode, "bid"));
    details.setAsk(getDoubleValue(allNode, "ask"));
    details.setBidSize(getLongValue(allNode, "bidSize"));
    details.setAskSize(getLongValue(allNode, "askSize"));
    details.setBidExchange(getStringValue(allNode, "bidExchange"));
    details.setBidTime(getStringValue(allNode, "bidTime"));
    details.setAskTime(getStringValue(allNode, "askTime"));
    
    // Parse company information
    details.setCompanyName(getStringValue(allNode, "companyName"));
    
    // Parse dividend information
    details.setDividend(getDoubleValue(allNode, "dividend"));
    details.setExDividendDate(getLongValue(allNode, "exDividendDate"));
    details.setNextEarningDate(getLongValue(allNode, "nextEarningDate"));
    
    // Parse earnings information
    details.setEps(getDoubleValue(allNode, "eps"));
    details.setEstEarnings(getDoubleValue(allNode, "estEarnings"));
    
    // Parse option-specific fields
    details.setAdjustedFlag(getBooleanValue(allNode, "adjustedFlag"));
    details.setDaysToExpiration(getIntValue(allNode, "daysToExpiration"));
    details.setOptionStyle(getStringValue(allNode, "optionStyle"));
    details.setOptionUnderlier(getStringValue(allNode, "optionUnderlier"));
    details.setOptionUnderlierExchange(getStringValue(allNode, "optionUnderlierExchange"));
    details.setOpenInterest(getLongValue(allNode, "openInterest"));
    
    // Parse Option Greeks
    JsonNode optionGreeksNode = allNode.path("OptionGreeks");
    if (!optionGreeksNode.isMissingNode()) {
      OptionGreeksDto greeks = new OptionGreeksDto();
      greeks.setRho(getDoubleValue(optionGreeksNode, "rho"));
      greeks.setVega(getDoubleValue(optionGreeksNode, "vega"));
      greeks.setTheta(getDoubleValue(optionGreeksNode, "theta"));
      greeks.setDelta(getDoubleValue(optionGreeksNode, "delta"));
      greeks.setGamma(getDoubleValue(optionGreeksNode, "gamma"));
      greeks.setIv(getDoubleValue(optionGreeksNode, "iv"));
      greeks.setCurrentValue(getBooleanValue(optionGreeksNode, "currentValue"));
      details.setOptionGreeks(greeks);
    }
    
    // Parse additional fields
    details.setQuoteType(getStringValue(allNode, "quoteType"));
    
    // Parse SelectedED
    JsonNode selectedEDNode = allNode.path("SelectedED");
    if (!selectedEDNode.isMissingNode()) {
      SelectedEDDto selectedED = new SelectedEDDto();
      selectedED.setMonth(getIntValue(selectedEDNode, "month"));
      selectedED.setYear(getIntValue(selectedEDNode, "year"));
      selectedED.setDay(getIntValue(selectedEDNode, "day"));
      details.setSelectedED(selectedED);
    }
    
    // Parse MutualFund-specific fields
    details.setNetAssetValue(getDoubleValue(allNode, "netAssetValue"));
    details.setPublicOfferPrice(getDoubleValue(allNode, "publicOfferPrice"));
    
    return details;
  }

  /**
   * Parses LookupProductResponse JSON node into LookupProductResponse DTO.
   */
  private LookupProductResponse parseLookupProductResponse(JsonNode lookupResponseNode) {
    LookupProductResponse response = new LookupProductResponse();
    
    // Parse Data array
    JsonNode dataNode = lookupResponseNode.path("Data");
    List<LookupProductDto> products = new ArrayList<>();
    if (dataNode.isArray()) {
      for (JsonNode productNode : dataNode) {
        products.add(parseLookupProduct(productNode));
      }
    } else if (dataNode.isObject() && !dataNode.isMissingNode()) {
      products.add(parseLookupProduct(dataNode));
    }
    response.setData(products);
    
    return response;
  }

  /**
   * Parses LookupProduct JSON node into LookupProductDto DTO.
   */
  private LookupProductDto parseLookupProduct(JsonNode productNode) {
    LookupProductDto productDto = new LookupProductDto();
    productDto.setSymbol(getStringValue(productNode, "symbol"));
    productDto.setDescription(getStringValue(productNode, "description"));
    productDto.setType(getStringValue(productNode, "type"));
    return productDto;
  }

  /**
   * Parses OptionChainResponse JSON node into OptionChainResponse DTO.
   */
  private OptionChainResponse parseOptionChainResponse(JsonNode optionChainNode) {
    OptionChainResponse response = new OptionChainResponse();
    
    response.setSymbol(getStringValue(optionChainNode, "symbol"));
    response.setNearPrice(getDoubleValue(optionChainNode, "nearPrice"));
    response.setAdjustedFlag(getBooleanValue(optionChainNode, "adjustedFlag"));
    response.setOptionChainType(getStringValue(optionChainNode, "optionChainType"));
    response.setTimestamp(getLongValue(optionChainNode, "timestamp"));
    response.setQuoteType(getStringValue(optionChainNode, "quoteType"));
    
    // Parse SelectedED
    JsonNode selectedEDNode = optionChainNode.path("SelectedED");
    if (!selectedEDNode.isMissingNode()) {
      SelectedEDDto selectedED = new SelectedEDDto();
      selectedED.setMonth(getIntValue(selectedEDNode, "month"));
      selectedED.setYear(getIntValue(selectedEDNode, "year"));
      selectedED.setDay(getIntValue(selectedEDNode, "day"));
      response.setSelectedED(selectedED);
    }
    
    // Parse OptionPair array
    JsonNode optionPairNode = optionChainNode.path("OptionPair");
    if (!optionPairNode.isMissingNode()) {
      List<OptionPairDto> pairs = new ArrayList<>();
      if (optionPairNode.isArray()) {
        for (JsonNode pairNode : optionPairNode) {
          pairs.add(parseOptionPair(pairNode));
        }
      } else {
        pairs.add(parseOptionPair(optionPairNode));
      }
      response.setOptionPairs(pairs);
    }
    
    return response;
  }

  /**
   * Parses OptionPair JSON node into OptionPairDto DTO.
   */
  private OptionPairDto parseOptionPair(JsonNode pairNode) {
    OptionPairDto pair = new OptionPairDto();
    
    pair.setStrikePrice(getDoubleValue(pairNode, "strikePrice"));
    
    // Parse Call option
    JsonNode callNode = pairNode.path("Call");
    if (!callNode.isMissingNode()) {
      pair.setCall(parseOption(callNode));
    }
    
    // Parse Put option
    JsonNode putNode = pairNode.path("Put");
    if (!putNode.isMissingNode()) {
      pair.setPut(parseOption(putNode));
    }
    
    return pair;
  }

  /**
   * Parses Option JSON node into OptionDto DTO.
   */
  private OptionDto parseOption(JsonNode optionNode) {
    OptionDto option = new OptionDto();
    
    option.setOptionCategory(getStringValue(optionNode, "optionCategory"));
    option.setOptionRootSymbol(getStringValue(optionNode, "optionRootSymbol"));
    option.setTimestamp(getLongValue(optionNode, "timestamp"));
    option.setAdjustedFlag(getBooleanValue(optionNode, "adjustedFlag"));
    option.setDisplaySymbol(getStringValue(optionNode, "displaySymbol"));
    option.setOptionType(getStringValue(optionNode, "optionType"));
    option.setStrikePrice(getDoubleValue(optionNode, "strikePrice"));
    option.setSymbol(getStringValue(optionNode, "symbol"));
    option.setBid(getDoubleValue(optionNode, "bid"));
    option.setAsk(getDoubleValue(optionNode, "ask"));
    option.setBidSize(getLongValue(optionNode, "bidSize"));
    option.setAskSize(getLongValue(optionNode, "askSize"));
    option.setInTheMoney(getStringValue(optionNode, "inTheMoney"));
    option.setVolume(getLongValue(optionNode, "volume"));
    option.setOpenInterest(getLongValue(optionNode, "openInterest"));
    option.setNetChange(getDoubleValue(optionNode, "netChange"));
    option.setLastPrice(getDoubleValue(optionNode, "lastPrice"));
    option.setQuoteDetail(getStringValue(optionNode, "quoteDetail"));
    option.setOsiKey(getStringValue(optionNode, "osiKey"));
    
    // Parse Option Greeks
    JsonNode optionGreeksNode = optionNode.path("OptionGreeks");
    if (!optionGreeksNode.isMissingNode()) {
      OptionGreeksDto greeks = new OptionGreeksDto();
      greeks.setRho(getDoubleValue(optionGreeksNode, "rho"));
      greeks.setVega(getDoubleValue(optionGreeksNode, "vega"));
      greeks.setTheta(getDoubleValue(optionGreeksNode, "theta"));
      greeks.setDelta(getDoubleValue(optionGreeksNode, "delta"));
      greeks.setGamma(getDoubleValue(optionGreeksNode, "gamma"));
      greeks.setIv(getDoubleValue(optionGreeksNode, "iv"));
      greeks.setCurrentValue(getBooleanValue(optionGreeksNode, "currentValue"));
      option.setOptionGreeks(greeks);
    }
    
    return option;
  }

  /**
   * Parses OptionExpireDateResponse JSON node into OptionExpireDateResponse DTO.
   */
  private OptionExpireDateResponse parseOptionExpireDateResponse(JsonNode expireDateResponseNode) {
    OptionExpireDateResponse response = new OptionExpireDateResponse();
    
    // Parse ExpireDate array
    JsonNode expireDateNode = expireDateResponseNode.path("ExpireDate");
    List<OptionExpireDateDto> expireDates = new ArrayList<>();
    if (expireDateNode.isArray()) {
      for (JsonNode dateNode : expireDateNode) {
        expireDates.add(parseExpireDate(dateNode));
      }
    } else if (expireDateNode.isObject() && !expireDateNode.isMissingNode()) {
      expireDates.add(parseExpireDate(expireDateNode));
    }
    response.setExpireDates(expireDates);
    
    return response;
  }

  /**
   * Parses ExpireDate JSON node into OptionExpireDateDto DTO.
   */
  private OptionExpireDateDto parseExpireDate(JsonNode dateNode) {
    OptionExpireDateDto date = new OptionExpireDateDto();
    date.setYear(getIntValue(dateNode, "year"));
    date.setMonth(getIntValue(dateNode, "month"));
    date.setDay(getIntValue(dateNode, "day"));
    date.setExpiryType(getStringValue(dateNode, "expiryType"));
    return date;
  }

  // ============================================================================
  // Helper methods for parsing JSON values
  // ============================================================================

  private String getStringValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    String text = fieldNode.asText();
    return (text != null && !text.isEmpty()) ? text : null;
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
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Double.parseDouble(text);
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
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Integer.parseInt(text);
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
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Boolean getBooleanValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isBoolean()) {
      return fieldNode.asBoolean();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Boolean.parseBoolean(text);
    } catch (Exception e) {
      return null;
    }
  }
}
