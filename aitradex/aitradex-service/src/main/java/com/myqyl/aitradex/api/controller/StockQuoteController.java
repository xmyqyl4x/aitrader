package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.StockQuoteHistoryDto;
import com.myqyl.aitradex.api.dto.StockQuoteSearchDto;
import com.myqyl.aitradex.api.dto.UpdateStockQuoteReviewRequest;
import com.myqyl.aitradex.domain.StockQuoteSearch;
import com.myqyl.aitradex.service.MarketDataService;
import com.myqyl.aitradex.service.StockQuoteSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockQuoteController {

  private final StockQuoteSearchService searchService;
  private final MarketDataService marketDataService;

  public StockQuoteController(
      StockQuoteSearchService searchService, MarketDataService marketDataService) {
    this.searchService = searchService;
    this.marketDataService = marketDataService;
  }

  @GetMapping("/quotes/{symbol}")
  public MarketDataQuoteDto getQuote(
      @PathVariable @NotBlank String symbol,
      @RequestParam(value = "source", required = false) String source) {
    return marketDataService.latestQuote(symbol, source);
  }

  @GetMapping("/quotes/{symbol}/history")
  public List<StockQuoteHistoryDto> getHistory(
      @PathVariable @NotBlank String symbol,
      @RequestParam @NotBlank String range,
      @RequestParam(value = "source", required = false) String source) {
    // For MVP, return empty list or single current quote as history
    // In production, this would fetch historical data from provider
    MarketDataQuoteDto quote = marketDataService.latestQuote(symbol, source);
    return List.of(
        new StockQuoteHistoryDto(
            quote.asOf(),
            quote.open(),
            quote.high(),
            quote.low(),
            quote.close(),
            quote.volume()));
  }

  @PostMapping("/quotes/{symbol}/search")
  @ResponseStatus(HttpStatus.CREATED)
  public StockQuoteSearchDto searchAndSave(
      @PathVariable @NotBlank String symbol,
      @RequestParam @NotBlank String range,
      @RequestParam(value = "source", required = false) String source,
      @RequestParam(value = "userId", required = false) UUID userId) {
    return searchService.createAndExecuteSearch(symbol, range, source, userId);
  }

  @GetMapping("/searches")
  public Page<StockQuoteSearchDto> listSearches(
      @RequestParam(value = "symbol", required = false) String symbol,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "dateFrom", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime dateFrom,
      @RequestParam(value = "dateTo", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime dateTo,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
    Pageable pageable =
        PageRequest.of(
            page,
            size,
            Sort.by(
                direction.equalsIgnoreCase("ASC")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC,
                sort));

    StockQuoteSearch.SearchStatus statusEnum = null;
    if (status != null && !status.isBlank()) {
      try {
        statusEnum = StockQuoteSearch.SearchStatus.valueOf(status.toUpperCase());
      } catch (IllegalArgumentException ex) {
        // Ignore invalid status
      }
    }

    return searchService.listSearches(symbol, statusEnum, dateFrom, dateTo, pageable);
  }

  @GetMapping("/searches/{id}")
  public StockQuoteSearchDto getSearch(@PathVariable UUID id) {
    return searchService.getSearch(id);
  }

  @PostMapping("/searches/{id}/rerun")
  @ResponseStatus(HttpStatus.CREATED)
  public StockQuoteSearchDto rerunSearch(
      @PathVariable UUID id, @RequestParam(value = "userId", required = false) UUID userId) {
    return searchService.rerunSearch(id, userId);
  }

  @PutMapping("/searches/{id}/review")
  public StockQuoteSearchDto updateReview(
      @PathVariable UUID id, @RequestBody @Valid UpdateStockQuoteReviewRequest request) {
    return searchService.updateReview(id, request);
  }
}
