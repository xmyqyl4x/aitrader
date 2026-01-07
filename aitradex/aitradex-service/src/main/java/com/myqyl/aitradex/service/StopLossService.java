package com.myqyl.aitradex.service;

import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderSource;
import com.myqyl.aitradex.domain.OrderStatus;
import com.myqyl.aitradex.domain.OrderType;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.repository.OrderRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StopLossService {

  private static final List<OrderStatus> OPEN_ORDER_STATUSES =
      List.of(OrderStatus.NEW, OrderStatus.ROUTED, OrderStatus.PARTIALLY_FILLED);

  private final PositionRepository positionRepository;
  private final OrderRepository orderRepository;
  private final MarketDataService marketDataService;

  public StopLossService(
      PositionRepository positionRepository,
      OrderRepository orderRepository,
      MarketDataService marketDataService) {
    this.positionRepository = positionRepository;
    this.orderRepository = orderRepository;
    this.marketDataService = marketDataService;
  }

  @Transactional
  public int enforceStopLosses(String source) {
    List<Position> positions = positionRepository.findByClosedAtIsNullAndStopLossIsNotNull();
    int triggered = 0;
    for (Position position : positions) {
      if (position.getQuantity() == null || position.getQuantity().signum() <= 0) {
        continue;
      }
      BigDecimal stopLoss = position.getStopLoss();
      if (stopLoss == null) {
        continue;
      }
      BigDecimal lastPrice =
          marketDataService.latestQuote(position.getSymbol(), source).close();
      if (lastPrice == null || lastPrice.compareTo(stopLoss) > 0) {
        continue;
      }

      boolean hasOpenOrder =
          orderRepository.existsByAccountIdAndSymbolAndStatusIn(
              position.getAccount().getId(), position.getSymbol(), OPEN_ORDER_STATUSES);
      if (hasOpenOrder) {
        continue;
      }

      Order order =
          Order.builder()
              .account(position.getAccount())
              .symbol(position.getSymbol())
              .side(OrderSide.SELL)
              .type(OrderType.MARKET)
              .status(OrderStatus.NEW)
              .quantity(position.getQuantity())
              .source(OrderSource.AUTOMATION)
              .stopPrice(stopLoss)
              .notes("Auto-triggered stop loss")
              .createdAt(OffsetDateTime.now())
              .build();
      orderRepository.save(order);
      triggered++;
    }

    return triggered;
  }
}
