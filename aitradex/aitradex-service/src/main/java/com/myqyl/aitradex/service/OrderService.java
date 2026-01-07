package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateOrderRequest;
import com.myqyl.aitradex.api.dto.OrderDto;
import com.myqyl.aitradex.api.dto.UpdateOrderStatusRequest;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderStatus;
import com.myqyl.aitradex.domain.OrderType;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.OrderRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final AccountRepository accountRepository;
  private final PositionRepository positionRepository;
  private final MarketDataService marketDataService;

  public OrderService(
      OrderRepository orderRepository,
      AccountRepository accountRepository,
      PositionRepository positionRepository,
      MarketDataService marketDataService) {
    this.orderRepository = orderRepository;
    this.accountRepository = accountRepository;
    this.positionRepository = positionRepository;
    this.marketDataService = marketDataService;
  }

  @Transactional
  public OrderDto create(CreateOrderRequest request) {
    Account account =
        accountRepository.findById(request.accountId()).orElseThrow(() -> accountNotFound(request.accountId()));
    validateOrder(request, account);

    Order order =
        Order.builder()
            .account(account)
            .symbol(request.symbol().toUpperCase())
            .side(request.side())
            .type(request.type())
            .status(OrderStatus.NEW)
            .limitPrice(request.limitPrice())
            .stopPrice(request.stopPrice())
            .quantity(request.quantity())
            .source(request.source())
            .notes(request.notes())
            .createdAt(OffsetDateTime.now())
            .build();
    return toDto(orderRepository.save(order));
  }

  @Transactional(readOnly = true)
  public List<OrderDto> list(UUID accountId, OrderStatus status) {
    List<Order> orders;
    if (accountId != null && status != null) {
      orders = orderRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(accountId, status);
    } else if (accountId != null) {
      orders = orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    } else if (status != null) {
      orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
    } else {
      orders = orderRepository.findAll();
    }
    return orders.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public OrderDto get(UUID id) {
    return orderRepository.findById(id).map(this::toDto).orElseThrow(() -> orderNotFound(id));
  }

  @Transactional
  public OrderDto updateStatus(UUID id, UpdateOrderStatusRequest request) {
    Order order = orderRepository.findById(id).orElseThrow(() -> orderNotFound(id));
    order.setStatus(request.status());

    if (request.routedAt() != null) {
      order.setRoutedAt(request.routedAt());
    } else if (request.status() == OrderStatus.ROUTED && order.getRoutedAt() == null) {
      order.setRoutedAt(OffsetDateTime.now());
    }

    if (request.filledAt() != null) {
      order.setFilledAt(request.filledAt());
    } else if (request.status() == OrderStatus.FILLED && order.getFilledAt() == null) {
      order.setFilledAt(OffsetDateTime.now());
    }

    if (request.notes() != null) {
      order.setNotes(request.notes());
    }

    return toDto(orderRepository.save(order));
  }

  private OrderDto toDto(Order order) {
    return new OrderDto(
        order.getId(),
        order.getAccount().getId(),
        order.getSymbol(),
        order.getSide(),
        order.getType(),
        order.getStatus(),
        order.getSource(),
        order.getLimitPrice(),
        order.getStopPrice(),
        order.getQuantity(),
        order.getRoutedAt(),
        order.getFilledAt(),
        order.getNotes(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }

  private NotFoundException orderNotFound(UUID id) {
    return new NotFoundException("Order %s not found".formatted(id));
  }

  private NotFoundException accountNotFound(UUID id) {
    return new NotFoundException("Account %s not found".formatted(id));
  }

  private void validateOrder(CreateOrderRequest request, Account account) {
    if (request.quantity() == null || request.quantity().signum() <= 0) {
      throw new IllegalArgumentException("Order quantity must be positive");
    }
    BigDecimal notional = estimateNotional(request);
    if (request.side() == OrderSide.BUY) {
      if (account.getCashBalance().compareTo(notional) < 0) {
        throw new IllegalStateException("Insufficient cash balance for order");
      }
      return;
    }
    Position position =
        positionRepository
            .findByAccountIdAndSymbolAndClosedAtIsNull(account.getId(), request.symbol().toUpperCase())
            .orElseThrow(() -> new IllegalStateException("No open position to sell"));
    if (position.getQuantity().compareTo(request.quantity()) < 0) {
      throw new IllegalStateException("Order quantity exceeds available position size");
    }
  }

  private BigDecimal estimateNotional(CreateOrderRequest request) {
    BigDecimal price = null;
    if (request.type() == OrderType.LIMIT) {
      price = request.limitPrice();
    } else if (request.type() == OrderType.STOP) {
      price = request.stopPrice();
    } else if (request.type() == OrderType.STOP_LIMIT) {
      price = request.limitPrice() != null ? request.limitPrice() : request.stopPrice();
    }

    if (price == null && request.type() == OrderType.MARKET) {
      var quote = marketDataService.latestQuote(request.symbol());
      price = firstAvailable(quote.close(), quote.open(), quote.high(), quote.low());
    }

    if (price == null) {
      throw new IllegalArgumentException("Unable to estimate order price for validation");
    }
    return price.multiply(request.quantity());
  }

  private BigDecimal firstAvailable(BigDecimal... values) {
    for (BigDecimal value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
