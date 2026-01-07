package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateOrderRequest;
import com.myqyl.aitradex.api.dto.OrderDto;
import com.myqyl.aitradex.api.dto.UpdateOrderStatusRequest;
import com.myqyl.aitradex.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.create(request);
  }

  @GetMapping
  public List<OrderDto> listOrders() {
    return orderService.list();
  }

  @GetMapping("/{id}")
  public OrderDto getOrder(@PathVariable UUID id) {
    return orderService.get(id);
  }

  @PatchMapping("/{id}/status")
  public OrderDto updateStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request) {
    return orderService.updateStatus(id, request);
  }
}
