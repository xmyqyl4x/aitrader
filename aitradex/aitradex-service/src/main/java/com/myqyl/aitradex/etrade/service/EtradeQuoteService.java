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
}
