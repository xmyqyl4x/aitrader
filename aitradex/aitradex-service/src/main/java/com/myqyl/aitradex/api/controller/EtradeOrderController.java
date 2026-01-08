package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.EtradeOrderDto;
import com.myqyl.aitradex.etrade.service.EtradeOrderService;
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
   * Previews an order before placement.
   */
  @PostMapping("/preview")
  public ResponseEntity<Map<String, Object>> previewOrder(
      @RequestParam UUID accountId,
      @RequestBody Map<String, Object> orderRequest) {
    Map<String, Object> preview = orderService.previewOrder(accountId, orderRequest);
    return ResponseEntity.ok(preview);
  }

  /**
   * Places an order.
   */
  @PostMapping
  public ResponseEntity<EtradeOrderDto> placeOrder(
      @RequestParam UUID accountId,
      @RequestBody Map<String, Object> orderRequest) {
    EtradeOrderDto order = orderService.placeOrder(accountId, orderRequest);
    return ResponseEntity.ok(order);
  }

  /**
   * Gets orders for an account.
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
   * Previews a changed order (modifies an existing order).
   */
  @PutMapping("/{orderId}/preview")
  public ResponseEntity<Map<String, Object>> changePreviewOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId,
      @RequestBody Map<String, Object> orderRequest) {
    Map<String, Object> preview = orderService.changePreviewOrder(accountId, orderId, orderRequest);
    return ResponseEntity.ok(preview);
  }

  /**
   * Places a changed order (modifies and places an existing order).
   */
  @PutMapping("/{orderId}")
  public ResponseEntity<EtradeOrderDto> changePlaceOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId,
      @RequestBody Map<String, Object> orderRequest) {
    EtradeOrderDto order = orderService.changePlaceOrder(accountId, orderId, orderRequest);
    return ResponseEntity.ok(order);
  }

  /**
   * Cancels an order.
   */
  @DeleteMapping("/{orderId}")
  public ResponseEntity<EtradeOrderDto> cancelOrder(
      @RequestParam UUID accountId,
      @PathVariable UUID orderId) {
    EtradeOrderDto order = orderService.cancelOrder(accountId, orderId);
    return ResponseEntity.ok(order);
  }
}
