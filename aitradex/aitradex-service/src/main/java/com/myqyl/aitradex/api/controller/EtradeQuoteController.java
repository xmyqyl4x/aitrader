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

  /**
   * Looks up products by symbol or company name.
   */
  @GetMapping("/lookup")
  public ResponseEntity<List<Map<String, Object>>> lookupProduct(
      @RequestParam String input) {
    List<Map<String, Object>> products = quoteService.lookupProduct(input);
    return ResponseEntity.ok(products);
  }

  /**
   * Gets option chains for a symbol.
   */
  @GetMapping("/option-chains")
  public ResponseEntity<Map<String, Object>> getOptionChains(
      @RequestParam String symbol,
      @RequestParam(required = false) Integer expiryYear,
      @RequestParam(required = false) Integer expiryMonth,
      @RequestParam(required = false) Integer expiryDay,
      @RequestParam(required = false) Integer strikePriceNear,
      @RequestParam(required = false) Integer noOfStrikes,
      @RequestParam(required = false) String optionCategory,
      @RequestParam(required = false) String chainType) {
    Map<String, Object> chains = quoteService.getOptionChains(symbol, expiryYear, expiryMonth,
        expiryDay, strikePriceNear, noOfStrikes, optionCategory, chainType);
    return ResponseEntity.ok(chains);
  }

  /**
   * Gets option expire dates for a symbol.
   */
  @GetMapping("/option-expire-dates")
  public ResponseEntity<List<Map<String, Object>>> getOptionExpireDates(
      @RequestParam String symbol) {
    List<Map<String, Object>> dates = quoteService.getOptionExpireDates(symbol);
    return ResponseEntity.ok(dates);
  }
}
