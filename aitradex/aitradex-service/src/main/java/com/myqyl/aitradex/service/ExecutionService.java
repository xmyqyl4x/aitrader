package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateExecutionRequest;
import com.myqyl.aitradex.api.dto.ExecutionDto;
import com.myqyl.aitradex.domain.Execution;
import com.myqyl.aitradex.domain.Order;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.ExecutionRepository;
import com.myqyl.aitradex.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {

  private final ExecutionRepository executionRepository;
  private final OrderRepository orderRepository;

  public ExecutionService(
      ExecutionRepository executionRepository, OrderRepository orderRepository) {
    this.executionRepository = executionRepository;
    this.orderRepository = orderRepository;
  }

  @Transactional
  public ExecutionDto create(CreateExecutionRequest request) {
    Order order =
        orderRepository.findById(request.orderId()).orElseThrow(() -> orderNotFound(request.orderId()));

    Execution execution =
        Execution.builder()
            .order(order)
            .price(request.price())
            .quantity(request.quantity())
            .venue(request.venue())
            .executedAt(request.executedAt() != null ? request.executedAt() : OffsetDateTime.now())
            .build();

    return toDto(executionRepository.save(execution));
  }

  @Transactional(readOnly = true)
  public List<ExecutionDto> list() {
    return executionRepository.findAll().stream().map(this::toDto).toList();
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
}
