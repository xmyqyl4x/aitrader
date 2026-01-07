package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateStockQuoteSearchRequest;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.StockQuoteSearchDto;
import com.myqyl.aitradex.api.dto.UpdateStockQuoteReviewRequest;
import com.myqyl.aitradex.domain.StockQuoteSearch;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.StockQuoteSearchRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockQuoteSearchService {

  private final StockQuoteSearchRepository repository;
  private final MarketDataService marketDataService;

  public StockQuoteSearchService(
      StockQuoteSearchRepository repository, MarketDataService marketDataService) {
    this.repository = repository;
    this.marketDataService = marketDataService;
  }

  @Transactional
  public StockQuoteSearchDto createSearch(CreateStockQuoteSearchRequest request) {
    StockQuoteSearch search =
        StockQuoteSearch.builder()
            .symbol(request.symbol().toUpperCase())
            .companyName(request.companyName())
            .exchange(request.exchange())
            .range(StockQuoteSearch.QuoteRange.fromValue(request.range()))
            .requestedAt(request.requestedAt())
            .status(
                request.status() != null
                    ? StockQuoteSearch.SearchStatus.valueOf(request.status())
                    : StockQuoteSearch.SearchStatus.SUCCESS)
            .quoteTimestamp(request.quoteTimestamp())
            .price(request.price())
            .currency(request.currency())
            .changeAmount(request.changeAmount())
            .changePercent(request.changePercent())
            .volume(request.volume())
            .provider(request.provider())
            .requestId(request.requestId())
            .correlationId(request.correlationId())
            .errorCode(request.errorCode())
            .errorMessage(request.errorMessage())
            .durationMs(request.durationMs())
            .reviewStatus(StockQuoteSearch.ReviewStatus.NOT_REVIEWED)
            .build();

    return StockQuoteSearchDto.fromEntity(repository.save(search));
  }

  @Transactional
  public StockQuoteSearchDto createAndExecuteSearch(
      String symbol, String range, String source, UUID userId) {
    long startTime = System.currentTimeMillis();
    StockQuoteSearch.SearchStatus status = StockQuoteSearch.SearchStatus.SUCCESS;
    String errorCode = null;
    String errorMessage = null;
    MarketDataQuoteDto quote = null;

    try {
      quote = marketDataService.latestQuote(symbol, source);
    } catch (Exception ex) {
      status = StockQuoteSearch.SearchStatus.FAILED;
      errorCode = ex.getClass().getSimpleName();
      errorMessage =
          ex.getMessage() != null && ex.getMessage().length() > 1000
              ? ex.getMessage().substring(0, 1000)
              : ex.getMessage();
    }

    long durationMs = System.currentTimeMillis() - startTime;

    StockQuoteSearch search =
        StockQuoteSearch.builder()
            .createdByUserId(userId)
            .symbol(symbol.toUpperCase())
            .range(StockQuoteSearch.QuoteRange.fromValue(range))
            .requestedAt(OffsetDateTime.now())
            .status(status)
            .quoteTimestamp(quote != null ? quote.asOf() : null)
            .price(quote != null ? quote.close() : null)
            .changeAmount(null) // Calculate if needed
            .changePercent(null) // Calculate if needed
            .volume(quote != null ? quote.volume() : null)
            .provider(source != null ? source : "alphavantage")
            .correlationId(UUID.randomUUID().toString())
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .durationMs((int) durationMs)
            .reviewStatus(StockQuoteSearch.ReviewStatus.NOT_REVIEWED)
            .build();

    return StockQuoteSearchDto.fromEntity(repository.save(search));
  }

  @Transactional(readOnly = true)
  public Page<StockQuoteSearchDto> listSearches(
      String symbol,
      StockQuoteSearch.SearchStatus status,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      Pageable pageable) {
    Page<StockQuoteSearch> searches =
        repository.findByFilters(symbol, status, dateFrom, dateTo, pageable);
    return searches.map(StockQuoteSearchDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public StockQuoteSearchDto getSearch(UUID id) {
    return repository
        .findById(id)
        .map(StockQuoteSearchDto::fromEntity)
        .orElseThrow(() -> new NotFoundException("Stock quote search %s not found".formatted(id)));
  }

  @Transactional
  public StockQuoteSearchDto rerunSearch(UUID id, UUID userId) {
    StockQuoteSearch original = repository.findById(id).orElseThrow(
        () -> new NotFoundException("Stock quote search %s not found".formatted(id)));

    // Create a new search record with same parameters
    return createAndExecuteSearch(
        original.getSymbol(),
        original.getRange().getValue(),
        original.getProvider(),
        userId);
  }

  @Transactional
  public StockQuoteSearchDto updateReview(UUID id, UpdateStockQuoteReviewRequest request) {
    StockQuoteSearch search =
        repository
            .findById(id)
            .orElseThrow(
                () -> new NotFoundException("Stock quote search %s not found".formatted(id)));

    search.setReviewStatus(StockQuoteSearch.ReviewStatus.valueOf(request.reviewStatus()));
    search.setReviewNote(request.reviewNote());
    search.setReviewedAt(OffsetDateTime.now());

    return StockQuoteSearchDto.fromEntity(repository.save(search));
  }
}
