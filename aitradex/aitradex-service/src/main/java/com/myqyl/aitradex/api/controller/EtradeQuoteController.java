package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.etrade.market.dto.*;
import com.myqyl.aitradex.etrade.service.EtradeQuoteService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE quote operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New endpoints use Market API DTOs.
 */
@RestController
@RequestMapping("/api/etrade/quotes")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeQuoteController {

  private final EtradeQuoteService quoteService;

  public EtradeQuoteController(EtradeQuoteService quoteService) {
    this.quoteService = quoteService;
  }

  /**
   * Gets quote for a symbol using DTOs.
   */
  @GetMapping("/{symbol}")
  public ResponseEntity<EtradeQuoteModel> getQuote(
      @PathVariable String symbol,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String detailFlag,
      @RequestParam(required = false) Boolean requireEarningsDate,
      @RequestParam(required = false) Boolean overrideSymbolCount,
      @RequestParam(required = false) Boolean skipMiniOptionsCheck) {
    EtradeQuoteModel quote = quoteService.getQuote(accountId, symbol, detailFlag);
    return ResponseEntity.ok(quote);
  }

  /**
   * Gets quotes for multiple symbols using DTOs.
   */
  @GetMapping
  public ResponseEntity<QuoteResponse> getQuotes(
      @RequestParam String symbols,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) String detailFlag,
      @RequestParam(required = false) Boolean requireEarningsDate,
      @RequestParam(required = false) Boolean overrideSymbolCount,
      @RequestParam(required = false) Boolean skipMiniOptionsCheck) {
    GetQuotesRequest request = new GetQuotesRequest();
    request.setSymbols(symbols);
    request.setDetailFlag(detailFlag);
    request.setRequireEarningsDate(requireEarningsDate);
    request.setOverrideSymbolCount(overrideSymbolCount);
    request.setSkipMiniOptionsCheck(skipMiniOptionsCheck);
    
    QuoteResponse response = quoteService.getQuotes(accountId, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Looks up products by symbol or company name using DTOs.
   */
  @GetMapping("/lookup")
  public ResponseEntity<LookupProductResponse> lookupProduct(
      @RequestParam String input) {
    LookupProductRequest request = new LookupProductRequest();
    request.setInput(input);
    
    LookupProductResponse response = quoteService.lookupProduct(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets option chains for a symbol using DTOs.
   */
  @GetMapping("/option-chains")
  public ResponseEntity<OptionChainResponse> getOptionChains(
      @RequestParam String symbol,
      @RequestParam(required = false) Integer expiryYear,
      @RequestParam(required = false) Integer expiryMonth,
      @RequestParam(required = false) Integer expiryDay,
      @RequestParam(required = false) Integer strikePriceNear,
      @RequestParam(required = false) Integer noOfStrikes,
      @RequestParam(required = false) String optionCategory,
      @RequestParam(required = false) String chainType) {
    GetOptionChainsRequest request = new GetOptionChainsRequest();
    request.setSymbol(symbol);
    request.setExpiryYear(expiryYear);
    request.setExpiryMonth(expiryMonth);
    request.setExpiryDay(expiryDay);
    request.setStrikePriceNear(strikePriceNear);
    request.setNoOfStrikes(noOfStrikes);
    request.setOptionCategory(optionCategory);
    request.setChainType(chainType);
    
    OptionChainResponse response = quoteService.getOptionChains(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets option expire dates for a symbol using DTOs.
   */
  @GetMapping("/option-expire-dates")
  public ResponseEntity<OptionExpireDateResponse> getOptionExpireDates(
      @RequestParam String symbol,
      @RequestParam(required = false) String expiryType) {
    GetOptionExpireDatesRequest request = new GetOptionExpireDatesRequest();
    request.setSymbol(symbol);
    request.setExpiryType(expiryType);
    
    OptionExpireDateResponse response = quoteService.getOptionExpireDates(request);
    return ResponseEntity.ok(response);
  }
}
