package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.etrade.client.EtradeQuoteClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for E*TRADE quote operations.
 */
@Service
public class EtradeQuoteService {

  private final EtradeQuoteClient quoteClient;

  public EtradeQuoteService(EtradeQuoteClient quoteClient) {
    this.quoteClient = quoteClient;
  }

  /**
   * Gets quotes for one or more symbols.
   * 
   * @param accountId Internal account UUID (null for delayed quotes)
   * @param symbols One or more stock symbols
   * @param detailFlag Detail flag (e.g., "ALL", "FUNDAMENTAL", "INTRADAY") (optional)
   * @param requireEarningsDate Whether to include earnings date (optional)
   * @param overrideSymbolCount Override symbol count limit (optional)
   * @param skipMiniOptionsCheck Skip mini options check (optional)
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String[] symbols, String detailFlag,
                                              Boolean requireEarningsDate, Integer overrideSymbolCount,
                                              Boolean skipMiniOptionsCheck) {
    return quoteClient.getQuotes(accountId, symbols, detailFlag, requireEarningsDate, 
                                 overrideSymbolCount, skipMiniOptionsCheck);
  }

  /**
   * Gets quotes for one or more symbols (simplified version with defaults).
   */
  public List<Map<String, Object>> getQuotes(UUID accountId, String... symbols) {
    return quoteClient.getQuotes(accountId, symbols);
  }

  /**
   * Gets a single quote.
   */
  public Map<String, Object> getQuote(UUID accountId, String symbol) {
    List<Map<String, Object>> quotes = quoteClient.getQuotes(accountId, symbol);
    if (quotes.isEmpty()) {
      throw new RuntimeException("Quote not found for symbol: " + symbol);
    }
    return quotes.get(0);
  }

  /**
   * Looks up products by symbol or company name.
   */
  public List<Map<String, Object>> lookupProduct(String input) {
    return quoteClient.lookupProduct(input);
  }

  /**
   * Gets option chains for a symbol.
   */
  public Map<String, Object> getOptionChains(String symbol, Integer expiryYear, Integer expiryMonth,
      Integer expiryDay, Integer strikePriceNear, Integer noOfStrikes, String optionCategory, String chainType) {
    return quoteClient.getOptionChains(symbol, expiryYear, expiryMonth, expiryDay, 
        strikePriceNear, noOfStrikes, optionCategory, chainType);
  }

  /**
   * Gets option expire dates for a symbol.
   * 
   * @param symbol Stock symbol
   * @param expiryType Expiry type filter (e.g., "WEEKLY", "MONTHLY") (optional)
   */
  public List<Map<String, Object>> getOptionExpireDates(String symbol, String expiryType) {
    return quoteClient.getOptionExpireDates(symbol, expiryType);
  }

  /**
   * Gets option expire dates for a symbol (simplified version).
   */
  public List<Map<String, Object>> getOptionExpireDates(String symbol) {
    return quoteClient.getOptionExpireDates(symbol);
  }
}
