package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.EtradeOrderDto;
import com.myqyl.aitradex.etrade.orders.dto.*;
import com.myqyl.aitradex.etrade.service.EtradeOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE order operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New endpoints use Order API DTOs.
 */
@RestController
@RequestMapping("/api/etrade/orders")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeOrderController {

  private static final Logger log = LoggerFactory.getLogger(EtradeOrderController.class);

  private final EtradeOrderService orderService;

  public EtradeOrderController(EtradeOrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * Previews an order before placement using DTOs.
   */
  @PostMapping("/preview")
  public ResponseEntity<PreviewOrderResponse> previewOrder(
      @RequestParam UUID accountId,
      @Valid @RequestBody PreviewOrderRequest request) {
    PreviewOrderResponse preview = orderService.previewOrder(accountId, request);
    return ResponseEntity.ok(preview);
  }

  /**
   * Places an order using DTOs.
   */
  @PostMapping
  public ResponseEntity<EtradeOrderDto> placeOrder(
      @RequestParam UUID accountId,
      @Valid @RequestBody PlaceOrderRequest request) {
    EtradeOrderDto order = orderService.placeOrder(accountId, request);
    return ResponseEntity.ok(order);
  }

  /**
   * Lists orders for an account from E*TRADE API using DTOs.
   */
  @GetMapping("/list")
  public ResponseEntity<OrdersResponse> listOrders(
      @RequestParam UUID accountId,
      @ModelAttribute ListOrdersRequest request) {
    OrdersResponse orders = orderService.listOrders(accountId, request);
    return ResponseEntity.ok(orders);
  }

  /**
   * Gets orders for an account from database (paged).
   */
  @GetMapping
  public ResponseEntity<Page<EtradeOrderDto>> getOrders(
      @RequestParam UUID accountId,
      @PageableDefault(size = 20) Pageable pageable) {
    Page<EtradeOrderDto> orders = orderService.getOrders(accountId, pageable);
    return ResponseEntity.ok(orders);
  }

  /**
   * Gets order details.
   */
  @GetMapping("/{orderId}")
  public ResponseEntity<EtradeOrderDto> getOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId) {
    EtradeOrderDto order = orderService.getOrder(accountId, orderId);
    return ResponseEntity.ok(order);
  }

  /**
   * Previews a changed order (modifies an existing order) using DTOs.
   */
  @PutMapping("/{orderId}/preview")
  public ResponseEntity<PreviewOrderResponse> changePreviewOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId,
      @Valid @RequestBody PreviewOrderRequest request) {
    PreviewOrderResponse preview = orderService.changePreviewOrder(accountId, orderId, request);
    return ResponseEntity.ok(preview);
  }

  /**
   * Places a changed order (modifies and places an existing order) using DTOs.
   */
  @PutMapping("/{orderId}")
  public ResponseEntity<EtradeOrderDto> placeChangedOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId,
      @Valid @RequestBody PlaceOrderRequest request) {
    EtradeOrderDto order = orderService.placeChangedOrder(accountId, orderId, request);
    return ResponseEntity.ok(order);
  }

  /**
   * Cancels an order using DTOs.
   */
  @DeleteMapping("/{orderId}")
  public ResponseEntity<CancelOrderResponse> cancelOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId) {
    CancelOrderResponse response = orderService.cancelOrder(accountId, orderId);
    return ResponseEntity.ok(response);
  }
}
