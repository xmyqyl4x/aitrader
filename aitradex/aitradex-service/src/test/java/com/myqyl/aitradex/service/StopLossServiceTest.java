package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.repository.OrderRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StopLossServiceTest {

  @Mock private PositionRepository positionRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private MarketDataService marketDataService;
  @Mock private AuditLogService auditLogService;

  private StopLossService stopLossService;

  @BeforeEach
  void setUp() {
    stopLossService =
        new StopLossService(positionRepository, orderRepository, marketDataService, auditLogService);
  }

  @Test
  void enforceStopLossesCreatesOrderAndAuditLog() {
    Position position = position();
    when(positionRepository.findByClosedAtIsNullAndStopLossIsNotNull())
        .thenReturn(List.of(position));
    when(orderRepository.existsByAccountIdAndSymbolAndStatusIn(any(), any(), any()))
        .thenReturn(false);
    when(marketDataService.latestQuote(position.getSymbol(), "quote-snapshots"))
        .thenReturn(
            new MarketDataQuoteDto(
                position.getSymbol(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                BigDecimal.valueOf(90),
                null,
                "quote-snapshots"));

    int triggered = stopLossService.enforceStopLosses("quote-snapshots");

    assertEquals(1, triggered);
    verify(orderRepository).save(any());
    verify(auditLogService).create(any());
  }

  private Position position() {
    Account account = Account.builder().id(UUID.randomUUID()).baseCurrency("USD").build();
    return Position.builder()
        .id(UUID.randomUUID())
        .account(account)
        .symbol("AAPL")
        .quantity(BigDecimal.valueOf(5))
        .stopLoss(BigDecimal.valueOf(95))
        .openedAt(OffsetDateTime.now())
        .build();
  }
}
