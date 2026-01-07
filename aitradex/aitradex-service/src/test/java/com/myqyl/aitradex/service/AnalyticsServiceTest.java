package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.SymbolPnlDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.PortfolioSnapshotRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

  @Mock private PortfolioSnapshotRepository snapshotRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private PositionRepository positionRepository;
  @Mock private MarketDataService marketDataService;

  private AnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    analyticsService =
        new AnalyticsService(
            snapshotRepository, accountRepository, positionRepository, marketDataService);
  }

  @Test
  void symbolPnlReturnsSortedResults() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(Account.builder().id(accountId).build()));
    when(positionRepository.findByAccountIdAndClosedAtIsNullOrderByOpenedAtDesc(accountId))
        .thenReturn(List.of(position("MSFT"), position("AAPL")));

    when(marketDataService.latestQuote("MSFT", null))
        .thenReturn(quote("MSFT", BigDecimal.valueOf(20)));
    when(marketDataService.latestQuote("AAPL", null))
        .thenReturn(quote("AAPL", BigDecimal.valueOf(10)));

    List<SymbolPnlDto> result = analyticsService.symbolPnl(accountId, null);

    assertEquals("AAPL", result.get(0).symbol());
    assertEquals("MSFT", result.get(1).symbol());
  }

  private Position position(String symbol) {
    return Position.builder()
        .account(Account.builder().id(UUID.randomUUID()).build())
        .symbol(symbol)
        .quantity(BigDecimal.ONE)
        .costBasis(BigDecimal.valueOf(5))
        .build();
  }

  private MarketDataQuoteDto quote(String symbol, BigDecimal close) {
    return new MarketDataQuoteDto(
        symbol, OffsetDateTime.now(), null, null, null, close, null, "quote-snapshots");
  }
}
