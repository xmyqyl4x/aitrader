package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.etrade.client.EtradeApiClientMarketAPI;
import com.myqyl.aitradex.etrade.client.EtradeQuoteClient;
import com.myqyl.aitradex.etrade.market.dto.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for E*TRADE quote operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New methods use EtradeApiClientMarketAPI with DTOs.
 * Old methods are deprecated and delegate to new methods where possible.
 */
@Service
public class EtradeQuoteService {

  private final EtradeQuoteClient quoteClient; // Deprecated - use marketApi instead
  private final EtradeApiClientMarketAPI marketApi; // New API client with DTOs

  public EtradeQuoteService(EtradeQuoteClient quoteClient, EtradeApiClientMarketAPI marketApi) {
    this.quoteClient = quoteClient;
    this.marketApi = marketApi;
  }

  /**
   * Gets quotes for one or more symbols using DTOs.
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param request GetQuotesRequest DTO containing symbols and query parameters
   * @return QuoteResponse DTO containing list of quotes
   */
  public QuoteResponse getQuotes(UUID accountId, GetQuotesRequest request) {
    return marketApi.getQuotes(accountId, request);
  }

  /**
   * Gets quotes for one or more symbols (deprecated - uses Maps).
   * @deprecated Use {@link #getQuotes(UUID, GetQuotesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getQuotes(UUID accountId, String[] symbols, String detailFlag,
                                              Boolean requireEarningsDate, Integer overrideSymbolCount,
                                              Boolean skipMiniOptionsCheck) {
    return quoteClient.getQuotes(accountId, symbols, detailFlag, requireEarningsDate, 
                                 overrideSymbolCount, skipMiniOptionsCheck);
  }

  /**
   * Gets quotes for one or more symbols (deprecated - simplified version with defaults).
   * @deprecated Use {@link #getQuotes(UUID, GetQuotesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    return quoteClient.getQuotes(accountId, symbols);
  }

  /**
   * Gets a single quote using DTOs.
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param symbol Stock symbol
   * @param detailFlag Detail flag (optional)
   * @return EtradeQuoteModel DTO containing quote details
   */
  public EtradeQuoteModel getQuote(UUID accountId, String symbol, String detailFlag) {
    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols(symbol);
    if (detailFlag != null) {
      request.setDetailFlag(detailFlag);
    }
    
    QuoteResponse response = marketApi.getQuotes(accountId, request);
    if (response.getQuoteData().isEmpty()) {
      throw new RuntimeException("Quote not found for symbol: " + symbol);
    }
    return response.getQuoteData().get(0);
  }

  /**
   * Gets a single quote (deprecated - uses Maps).
   * @deprecated Use {@link #getQuote(UUID, String, String)} instead
   */
  @Deprecated
  public Map<String, Object> getQuote(UUID accountId, String symbol) {
    List<Map<String, Object>> quotes = quoteClient.getQuotes(accountId, symbol);
    if (quotes.isEmpty()) {
      throw new RuntimeException("Quote not found for symbol: " + symbol);
    }
    return quotes.get(0);
  }

  /**
   * Looks up products by symbol or company name using DTOs.
   * 
   * @param request LookupProductRequest DTO containing search input
   * @return LookupProductResponse DTO containing list of matching products
   */
  public LookupProductResponse lookupProduct(LookupProductRequest request) {
    return marketApi.lookupProduct(request);
  }

  /**
   * Looks up products by symbol or company name (deprecated - uses Maps).
   * @deprecated Use {@link #lookupProduct(LookupProductRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> lookupProduct(String input) {
    return quoteClient.lookupProduct(input);
  }

  /**
   * Gets option chains for a symbol using DTOs.
   * 
   * @param request GetOptionChainsRequest DTO containing symbol and option chain parameters
   * @return OptionChainResponse DTO containing option chains
   */
  public OptionChainResponse getOptionChains(GetOptionChainsRequest request) {
    return marketApi.getOptionChains(request);
  }

  /**
   * Gets option chains for a symbol (deprecated - uses Maps).
   * @deprecated Use {@link #getOptionChains(GetOptionChainsRequest)} instead
   */
  @Deprecated
  public Map<String, Object> getOptionChains(String symbol, Integer expiryYear, Integer expiryMonth,
      Integer expiryDay, Integer strikePriceNear, Integer noOfStrikes, String optionCategory, String chainType) {
    return quoteClient.getOptionChains(symbol, expiryYear, expiryMonth, expiryDay, 
        strikePriceNear, noOfStrikes, optionCategory, chainType);
  }

  /**
   * Gets option expire dates for a symbol using DTOs.
   * 
   * @param request GetOptionExpireDatesRequest DTO containing symbol and expiry type filter
   * @return OptionExpireDateResponse DTO containing list of expiration dates
   */
  public OptionExpireDateResponse getOptionExpireDates(GetOptionExpireDatesRequest request) {
    return marketApi.getOptionExpireDates(request);
  }

  /**
   * Gets option expire dates for a symbol (deprecated - uses Maps).
   * @deprecated Use {@link #getOptionExpireDates(GetOptionExpireDatesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getOptionExpireDates(String symbol, String expiryType) {
    return quoteClient.getOptionExpireDates(symbol, expiryType);
  }

  /**
   * Gets option expire dates for a symbol (deprecated - simplified version).
   * @deprecated Use {@link #getOptionExpireDates(GetOptionExpireDatesRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> getOptionExpireDates(String symbol) {
    return quoteClient.getOptionExpireDates(symbol);
  }
}
