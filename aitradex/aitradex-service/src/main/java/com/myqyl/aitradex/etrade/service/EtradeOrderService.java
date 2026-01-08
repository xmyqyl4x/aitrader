package com.myqyl.aitradex.etrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.EtradeOrderDto;
import com.myqyl.aitradex.etrade.client.EtradeOrderClient;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.domain.EtradeOrder;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import com.myqyl.aitradex.etrade.repository.EtradeOrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing E*TRADE orders.
 */
@Service
public class EtradeOrderService {

  private static final Logger log = LoggerFactory.getLogger(EtradeOrderService.class);

  private final EtradeOrderRepository orderRepository;
  private final EtradeAccountRepository accountRepository;
  private final EtradeOrderClient orderClient;
  private final ObjectMapper objectMapper;

  public EtradeOrderService(
      EtradeOrderRepository orderRepository,
      EtradeAccountRepository accountRepository,
      EtradeOrderClient orderClient,
      ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.accountRepository = accountRepository;
    this.orderClient = orderClient;
    this.objectMapper = objectMapper;
  }

  /**
   * Previews an order before placement.
   */
  public Map<String, Object> previewOrder(UUID accountId, Map<String, Object> orderRequest) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    return orderClient.previewOrder(accountId, account.getAccountIdKey(), orderRequest);
  }

  /**
   * Places an order.
   */
  @Transactional
  public EtradeOrderDto placeOrder(UUID accountId, Map<String, Object> orderRequest) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    // Preview first
    Map<String, Object> preview = orderClient.previewOrder(accountId, account.getAccountIdKey(), orderRequest);

    // Place order
    Map<String, Object> placeResponse = orderClient.placeOrder(accountId, account.getAccountIdKey(), orderRequest);

    // Save order to database
    EtradeOrder order = new EtradeOrder();
    order.setAccountId(accountId);
    
    @SuppressWarnings("unchecked")
    List<String> orderIds = (List<String>) placeResponse.get("orderIds");
    if (orderIds != null && !orderIds.isEmpty()) {
      order.setEtradeOrderId(orderIds.get(0));
    }

    Map<String, Object> orderDetails = (Map<String, Object>) orderRequest.get("OrderDetail");
    if (orderDetails != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> instrument = (Map<String, Object>) orderDetails.get("Instrument");
      if (instrument != null) {
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) instrument.get("Product");
        if (product != null) {
          order.setSymbol((String) product.get("symbol"));
        }
        order.setQuantity(((Number) instrument.get("quantity")).intValue());
        order.setSide((String) instrument.get("orderAction"));
      }
      
      order.setOrderType((String) orderDetails.get("orderType"));
      order.setPriceType((String) orderDetails.get("priceType"));
      
      if (orderDetails.get("limitPrice") != null) {
        order.setLimitPrice(new BigDecimal(orderDetails.get("limitPrice").toString()));
      }
      if (orderDetails.get("stopPrice") != null) {
        order.setStopPrice(new BigDecimal(orderDetails.get("stopPrice").toString()));
      }
    }

    try {
      order.setPreviewData(objectMapper.writeValueAsString(preview));
      order.setOrderResponse(objectMapper.writeValueAsString(placeResponse));
    } catch (Exception e) {
      log.warn("Failed to serialize order data", e);
    }

    order.setOrderStatus("SUBMITTED");
    order.setPlacedAt(OffsetDateTime.now());

    EtradeOrder saved = orderRepository.save(order);
    log.info("Placed E*TRADE order {} for account {}", saved.getEtradeOrderId(), accountId);

    return toDto(saved);
  }

  /**
   * Gets orders for an account.
   * Fetches from E*TRADE API and syncs with database.
   */
  public Page<EtradeOrderDto> getOrders(UUID accountId, Pageable pageable) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    
    // Fetch orders from E*TRADE API
    List<Map<String, Object>> etradeOrders = orderClient.getOrders(accountId, account.getAccountIdKey(),
        null, null, null, null, null, null, null, null, null);
    
    // Sync with database (simplified - in production, you'd want to merge/update existing orders)
    // For now, return from database with pagination
    return orderRepository.findByAccountId(accountId, pageable)
        .map(this::toDto);
  }

  /**
   * Gets order details.
   */
  public EtradeOrderDto getOrder(UUID accountId, UUID orderId) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    return toDto(order);
  }

  /**
   * Cancels an order.
   */
  @Transactional
  public EtradeOrderDto cancelOrder(UUID accountId, UUID orderId) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    orderClient.cancelOrder(accountId, account.getAccountIdKey(), order.getEtradeOrderId());

    order.setOrderStatus("CANCELLED");
    order.setCancelledAt(OffsetDateTime.now());
    EtradeOrder saved = orderRepository.save(order);

    log.info("Cancelled E*TRADE order {}", order.getEtradeOrderId());

    return toDto(saved);
  }

  private EtradeOrderDto toDto(EtradeOrder order) {
    Map<String, Object> previewData = null;
    Map<String, Object> orderResponse = null;
    
    try {
      if (order.getPreviewData() != null) {
        previewData = objectMapper.readValue(order.getPreviewData(), Map.class);
      }
      if (order.getOrderResponse() != null) {
        orderResponse = objectMapper.readValue(order.getOrderResponse(), Map.class);
      }
    } catch (Exception e) {
      log.warn("Failed to parse order JSON data", e);
    }

    return new EtradeOrderDto(
        order.getId(),
        order.getAccountId(),
        order.getEtradeOrderId(),
        order.getSymbol(),
        order.getOrderType(),
        order.getPriceType(),
        order.getSide(),
        order.getQuantity(),
        order.getLimitPrice(),
        order.getStopPrice(),
        order.getOrderStatus(),
        order.getPlacedAt(),
        order.getExecutedAt(),
        order.getCancelledAt(),
        previewData,
        orderResponse);
  }
}
