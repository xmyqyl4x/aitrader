package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.myqyl.aitradex.api.dto.CreateOrderRequest;
import com.myqyl.aitradex.api.dto.MarketDataQuoteDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderSource;
import com.myqyl.aitradex.domain.OrderType;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.OrderRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private PositionRepository positionRepository;
  @Mock private MarketDataService marketDataService;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(orderRepository, accountRepository, positionRepository, marketDataService);
  }

  @Test
  void createLimitOrderRequiresLimitPrice() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(accountId)));

    CreateOrderRequest request =
        new CreateOrderRequest(
            accountId,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            OrderSource.MANUAL,
            null,
            null,
            BigDecimal.ONE,
            null);

    assertThrows(IllegalArgumentException.class, () -> orderService.create(request));
  }

  @Test
  void createStopOrderRequiresStopPrice() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(accountId)));

    CreateOrderRequest request =
        new CreateOrderRequest(
            accountId,
            "AAPL",
            OrderSide.BUY,
            OrderType.STOP,
            OrderSource.MANUAL,
            null,
            null,
            BigDecimal.ONE,
            null);

    assertThrows(IllegalArgumentException.class, () -> orderService.create(request));
  }

  @Test
  void createBuyOrderRejectsWhenCashIsInsufficient() {
    UUID accountId = UUID.randomUUID();
    Account account = account(accountId);
    account.setCashBalance(BigDecimal.ONE);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(marketDataService.latestQuote("AAPL"))
        .thenReturn(
            new MarketDataQuoteDto(
                "AAPL",
                OffsetDateTime.now(),
                null,
                null,
                null,
                BigDecimal.TEN,
                null,
                "quote-snapshots"));

    CreateOrderRequest request =
        new CreateOrderRequest(
            accountId,
            "AAPL",
            OrderSide.BUY,
            OrderType.MARKET,
            OrderSource.MANUAL,
            null,
            null,
            BigDecimal.valueOf(2),
            null);

    assertThrows(IllegalStateException.class, () -> orderService.create(request));
  }

  private Account account(UUID id) {
    return Account.builder()
        .id(id)
        .baseCurrency("USD")
        .cashBalance(BigDecimal.valueOf(1000))
        .build();
  }
}
