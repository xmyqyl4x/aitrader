package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateExecutionRequest;
import com.myqyl.aitradex.api.dto.ExecutionDto;
import com.myqyl.aitradex.domain.Account;
import com.myqyl.aitradex.domain.Execution;
import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderStatus;
import com.myqyl.aitradex.domain.Position;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.ExecutionRepository;
import com.myqyl.aitradex.repository.OrderRepository;
import com.myqyl.aitradex.repository.PositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {

  private final ExecutionRepository executionRepository;
  private final OrderRepository orderRepository;
  private final PositionRepository positionRepository;

  public ExecutionService(
      ExecutionRepository executionRepository,
      OrderRepository orderRepository,
      PositionRepository positionRepository) {
    this.executionRepository = executionRepository;
    this.orderRepository = orderRepository;
    this.positionRepository = positionRepository;
  }

  @Transactional
  public ExecutionDto create(CreateExecutionRequest request) {
    Order order =
        orderRepository
            .findById(request.orderId())
            .orElseThrow(() -> orderNotFound(request.orderId()));
    ensureExecutable(order);

    Execution execution =
        Execution.builder()
            .order(order)
            .price(request.price())
            .quantity(request.quantity())
            .venue(request.venue())
            .executedAt(request.executedAt() != null ? request.executedAt() : OffsetDateTime.now())
            .build();

    Execution saved = executionRepository.save(execution);
    applyExecutionToAccount(order, saved);
    applyExecutionToPosition(order, saved);
    updateOrderStatus(order);

    return toDto(saved);
  }

  @Transactional(readOnly = true)
  public List<ExecutionDto> list(UUID orderId) {
    List<Execution> executions =
        orderId != null
            ? executionRepository.findByOrderIdOrderByExecutedAtDesc(orderId)
            : executionRepository.findAll();
    return executions.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public ExecutionDto get(UUID id) {
    return executionRepository.findById(id).map(this::toDto).orElseThrow(() -> executionNotFound(id));
  }

  private ExecutionDto toDto(Execution execution) {
    return new ExecutionDto(
        execution.getId(),
        execution.getOrder().getId(),
        execution.getPrice(),
        execution.getQuantity(),
        execution.getVenue(),
        execution.getExecutedAt());
  }

  private NotFoundException executionNotFound(UUID id) {
    return new NotFoundException("Execution %s not found".formatted(id));
  }

  private NotFoundException orderNotFound(UUID id) {
    return new NotFoundException("Order %s not found".formatted(id));
  }

  private void ensureExecutable(Order order) {
    if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
      throw new IllegalStateException(
          "Order %s is not executable in status %s".formatted(order.getId(), order.getStatus()));
    }
  }

  private void applyExecutionToAccount(Order order, Execution execution) {
    Account account = order.getAccount();
    BigDecimal fillValue = execution.getPrice().multiply(execution.getQuantity());
    BigDecimal cashBalance =
        order.getSide() == OrderSide.BUY
            ? account.getCashBalance().subtract(fillValue)
            : account.getCashBalance().add(fillValue);
    account.setCashBalance(cashBalance);
  }

  private void applyExecutionToPosition(Order order, Execution execution) {
    UUID accountId = order.getAccount().getId();
    String symbol = order.getSymbol();
    Optional<Position> existing =
        positionRepository.findByAccountIdAndSymbolAndClosedAtIsNull(accountId, symbol);

    if (order.getSide() == OrderSide.BUY) {
      Position position =
          existing.orElseGet(
              () ->
                  Position.builder()
                      .account(order.getAccount())
                      .symbol(symbol)
                      .quantity(BigDecimal.ZERO)
                      .costBasis(BigDecimal.ZERO)
                      .openedAt(execution.getExecutedAt())
                      .build());
      BigDecimal currentQuantity = position.getQuantity();
      BigDecimal currentCost = position.getCostBasis().multiply(currentQuantity);
      BigDecimal newQuantity = currentQuantity.add(execution.getQuantity());
      BigDecimal newCost =
          currentCost.add(execution.getPrice().multiply(execution.getQuantity()));
      position.setQuantity(newQuantity);
      position.setCostBasis(newCost.divide(newQuantity, 8, RoundingMode.HALF_UP));
      positionRepository.save(position);
      return;
    }

    if (existing.isEmpty()) {
      return;
    }
    Position position = existing.get();
    BigDecimal newQuantity = position.getQuantity().subtract(execution.getQuantity());
    if (newQuantity.signum() <= 0) {
      position.setQuantity(BigDecimal.ZERO);
      position.setClosedAt(execution.getExecutedAt());
    } else {
      position.setQuantity(newQuantity);
    }
    positionRepository.save(position);
  }

  private void updateOrderStatus(Order order) {
    List<Execution> executions =
        executionRepository.findByOrderIdOrderByExecutedAtDesc(order.getId());
    BigDecimal filledQuantity =
        executions.stream().map(Execution::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (order.getRoutedAt() == null) {
      order.setRoutedAt(OffsetDateTime.now());
    }
    if (filledQuantity.compareTo(order.getQuantity()) >= 0) {
      order.setStatus(OrderStatus.FILLED);
      if (order.getFilledAt() == null) {
        order.setFilledAt(OffsetDateTime.now());
      }
    } else {
      order.setStatus(OrderStatus.PARTIALLY_FILLED);
    }
  }
}
