package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.service.MarketDataService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

  private final MarketDataService marketDataService;

  public MarketDataController(MarketDataService marketDataService) {
    this.marketDataService = marketDataService;
  }

  @GetMapping("/quotes/latest")
  public MarketDataQuoteDto latestQuote(
      @RequestParam("symbol") @NotBlank String symbol,
      @RequestParam(value = "source", required = false) String source) {
    return marketDataService.latestQuote(symbol, source);
  }

  @GetMapping("/sources")
  public List<String> sources() {
    return marketDataService.listSources();
  }

  @GetMapping("/health")
  public Map<String, Long> health() {
    return marketDataService.sourceCounts();
  }
}
