package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.etrade.service.EtradeQuoteService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE quote operations.
 */
@RestController
@RequestMapping("/api/etrade/quotes")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeQuoteController {

  private static final Logger log = LoggerFactory.getLogger(EtradeQuoteController.class);

  private final EtradeQuoteService quoteService;

  public EtradeQuoteController(EtradeQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  /**
   * Gets quote for a symbol.
   */
  @GetMapping("/{symbol}")
  public ResponseEntity<Map<String, Object>> getQuote(
      @PathVariable String symbol,
      @RequestParam UUID accountId) {
    Map<String, Object> quote = quoteService.getQuote(accountId, symbol);
    return ResponseEntity.ok(quote);
  }

  /**
   * Gets quotes for multiple symbols.
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> getQuotes(
      @RequestParam String symbols,
      @RequestParam UUID accountId) {
    String[] symbolArray = symbols.split(",");
    List<Map<String, Object>> quotes = quoteService.getQuotes(accountId, symbolArray);
    return ResponseEntity.ok(quotes);
  }
}
