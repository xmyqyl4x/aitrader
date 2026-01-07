package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateOrderRequest;
import com.myqyl.aitradex.api.dto.OrderDto;
import com.myqyl.aitradex.api.dto.UpdateOrderStatusRequest;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.domain.OrderStatus;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AccountRepository;
import com.myqyl.aitradex.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final AccountRepository accountRepository;

  public OrderService(OrderRepository orderRepository, AccountRepository accountRepository) {
    this.orderRepository = orderRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public OrderDto create(CreateOrderRequest request) {
    Account account =
        accountRepository.findById(request.accountId()).orElseThrow(() -> accountNotFound(request.accountId()));

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
}
