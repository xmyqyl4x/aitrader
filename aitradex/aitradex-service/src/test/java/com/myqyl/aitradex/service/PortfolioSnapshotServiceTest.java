package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.api.dto.PortfolioSnapshotDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.PortfolioSnapshot;
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
class PortfolioSnapshotServiceTest {

  @Mock private PortfolioSnapshotRepository snapshotRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private PositionRepository positionRepository;
  @Mock private MarketDataService marketDataService;

  private PortfolioSnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    snapshotService =
        new PortfolioSnapshotService(
            snapshotRepository, accountRepository, positionRepository, marketDataService);
  }

  @Test
  void createSnapshotForAccountComputesEquityAndPnl() {
    UUID accountId = UUID.randomUUID();
    Account account =
        Account.builder().id(accountId).baseCurrency("USD").cashBalance(BigDecimal.valueOf(1000)).build();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(positionRepository.findByAccountIdAndClosedAtIsNullOrderByOpenedAtDesc(accountId))
        .thenReturn(List.of(position(account)));
    when(snapshotRepository.findTopByAccountIdOrderByEquityDesc(accountId))
        .thenReturn(Optional.empty());
    when(snapshotRepository.save(any(PortfolioSnapshot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(marketDataService.latestQuote("AAPL", "quote-snapshots"))
        .thenReturn(
            new MarketDataQuoteDto(
                "AAPL",
                OffsetDateTime.now(),
                null,
                null,
                null,
                BigDecimal.valueOf(12),
                null,
                "quote-snapshots"));

    PortfolioSnapshotDto snapshot = snapshotService.createSnapshotForAccount(accountId, "quote-snapshots");

    assertEquals(0, snapshot.equity().compareTo(BigDecimal.valueOf(1024.0000)));
    assertEquals(0, snapshot.pnl().compareTo(BigDecimal.valueOf(4.0000)));
  }

  private Position position(Account account) {
    return Position.builder()
        .account(account)
        .symbol("AAPL")
        .quantity(BigDecimal.valueOf(2))
        .costBasis(BigDecimal.valueOf(10))
        .openedAt(OffsetDateTime.now())
        .build();
  }
}
