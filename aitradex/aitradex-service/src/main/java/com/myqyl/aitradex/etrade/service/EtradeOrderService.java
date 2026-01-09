package com.myqyl.aitradex.etrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.EtradeOrderDto;
import com.myqyl.aitradex.etrade.client.EtradeApiClientOrderAPI;
import com.myqyl.aitradex.etrade.client.EtradeOrderClient;
import com.myqyl.aitradex.etrade.domain.EtradeAccount;
import com.myqyl.aitradex.etrade.domain.EtradeOrder;
import com.myqyl.aitradex.etrade.orders.dto.*;
import com.myqyl.aitradex.etrade.repository.EtradeAccountRepository;
import com.myqyl.aitradex.etrade.repository.EtradeOrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing E*TRADE orders.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New methods use EtradeApiClientOrderAPI with DTOs.
 * Old methods are deprecated and delegate to new methods where possible.
 */
@Service
public class EtradeOrderService {

  private static final Logger log = LoggerFactory.getLogger(EtradeOrderService.class);

  private final EtradeOrderRepository orderRepository;
  private final EtradeAccountRepository accountRepository;
  private final EtradeOrderClient orderClient; // Deprecated - use orderApi instead
  private final EtradeApiClientOrderAPI orderApi; // New API client with DTOs
  private final ObjectMapper objectMapper;

  public EtradeOrderService(
      EtradeOrderRepository orderRepository,
      EtradeAccountRepository accountRepository,
      EtradeOrderClient orderClient,
      EtradeApiClientOrderAPI orderApi,
      ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.accountRepository = accountRepository;
    this.orderClient = orderClient;
    this.orderApi = orderApi;
    this.objectMapper = objectMapper;
  }

  /**
   * Previews an order before placement using DTOs and persists preview attempt.
   * 
   * @param accountId Internal account UUID
   * @param request PreviewOrderRequest DTO
   * @return PreviewOrderResponse DTO
   */
  @Transactional(noRollbackFor = com.myqyl.aitradex.etrade.exception.EtradeApiException.class)
  public PreviewOrderResponse previewOrder(UUID accountId, PreviewOrderRequest request) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    PreviewOrderResponse response = orderApi.previewOrder(accountId, account.getAccountIdKey(), request);
    
    // Persist preview attempt (save preview record)
    persistPreviewAttempt(accountId, account.getAccountIdKey(), request, response);
    
    return response;
  }

  /**
   * Previews an order before placement (deprecated - uses Maps).
   * @deprecated Use {@link #previewOrder(UUID, PreviewOrderRequest)} instead
   */
  @Deprecated
  public Map<String, Object> previewOrder(UUID accountId, Map<String, Object> orderRequest) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    return orderClient.previewOrder(accountId, account.getAccountIdKey(), orderRequest);
  }

  /**
   * Places an order using DTOs.
   * 
   * @param accountId Internal account UUID
   * @param request PlaceOrderRequest DTO
   * @return EtradeOrderDto (internal order DTO for database entity)
   */
  @Transactional
  public EtradeOrderDto placeOrder(UUID accountId, PlaceOrderRequest request) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    // Build preview request from place request
    PreviewOrderRequest previewRequest = new PreviewOrderRequest();
    previewRequest.setOrderType(request.getOrderType());
    previewRequest.setClientOrderId(request.getClientOrderId());
    previewRequest.setOrders(request.getOrders());

    // Preview first
    PreviewOrderResponse preview = orderApi.previewOrder(accountId, account.getAccountIdKey(), previewRequest);

    // Use the first preview ID from the preview response
    if (preview.getPreviewIds().isEmpty()) {
      throw new RuntimeException("No preview ID returned from preview order");
    }
    String previewId = preview.getPreviewIds().get(0).getPreviewId();

    // Update place request with preview ID
    PlaceOrderRequest placeRequest = new PlaceOrderRequest();
    placeRequest.setPreviewId(previewId);
    placeRequest.setOrderType(request.getOrderType());
    placeRequest.setClientOrderId(request.getClientOrderId());
    placeRequest.setOrders(request.getOrders());

    // Place order
    PlaceOrderResponse placeResponse = orderApi.placeOrder(accountId, account.getAccountIdKey(), placeRequest);

    // Save order to database
    EtradeOrder order = new EtradeOrder();
    order.setAccountId(accountId);
    order.setAccountIdKey(account.getAccountIdKey());
    order.setClientOrderId(request.getClientOrderId());
    order.setPreviewId(previewId);
    order.setPreviewTime(OffsetDateTime.now());
    
    if (!placeResponse.getOrderIds().isEmpty()) {
      order.setEtradeOrderId(placeResponse.getOrderIds().get(0).getOrderId());
    }
    
    // Set placedTime from response
    if (placeResponse.getPlacedTime() != null) {
      order.setPlacedTime(placeResponse.getPlacedTime());
      order.setPlacedAt(OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(placeResponse.getPlacedTime()),
          java.time.ZoneOffset.UTC));
    } else {
      order.setPlacedAt(OffsetDateTime.now());
    }

    // Extract order details from request
    if (!request.getOrders().isEmpty()) {
      OrderDetailDto orderDetail = request.getOrders().get(0);
      order.setPriceType(orderDetail.getPriceType());
      order.setOrderType(request.getOrderType());
      
      if (orderDetail.getLimitPrice() != null) {
        order.setLimitPrice(BigDecimal.valueOf(orderDetail.getLimitPrice()));
      }
      if (orderDetail.getStopPrice() != null) {
        order.setStopPrice(BigDecimal.valueOf(orderDetail.getStopPrice()));
      }

      // Extract instrument details
      if (!orderDetail.getInstruments().isEmpty()) {
        OrderInstrumentDto instrument = orderDetail.getInstruments().get(0);
        if (instrument.getProduct() != null) {
          order.setSymbol(instrument.getProduct().getSymbol());
        }
        order.setQuantity(instrument.getQuantity());
        order.setSide(instrument.getOrderAction());
      }
    }

    try {
      order.setPreviewData(objectMapper.writeValueAsString(preview));
      order.setOrderResponse(objectMapper.writeValueAsString(placeResponse));
    } catch (Exception e) {
      log.warn("Failed to serialize order data", e);
    }

    order.setOrderStatus("SUBMITTED");

    EtradeOrder saved = orderRepository.save(order);
    log.info("Placed E*TRADE order {} for account {}", saved.getEtradeOrderId(), accountId);

    return toDto(saved);
  }

  /**
   * Places an order (deprecated - uses Maps).
   * @deprecated Use {@link #placeOrder(UUID, PlaceOrderRequest)} instead
   */
  @Deprecated
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
   * Gets orders for an account using DTOs and persists orders (upsert by orderId + accountIdKey).
   * 
   * @param accountId Internal account UUID
   * @param request ListOrdersRequest DTO containing query parameters
   * @return OrdersResponse DTO
   */
  @Transactional(noRollbackFor = com.myqyl.aitradex.etrade.exception.EtradeApiException.class)
  public OrdersResponse listOrders(UUID accountId, ListOrdersRequest request) {
    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    
    OrdersResponse response = orderApi.listOrders(accountId, account.getAccountIdKey(), request);
    
    // Persist orders (upsert by orderId + accountIdKey)
    persistOrders(accountId, account.getAccountIdKey(), response);
    
    return response;
  }

  /**
   * Gets orders for an account (paged from database).
   * Fetches from database with pagination.
   * 
   * @param accountId Internal account UUID
   * @param pageable Pagination parameters
   * @return Page of EtradeOrderDto (internal order DTOs)
   */
  public Page<EtradeOrderDto> getOrders(UUID accountId, Pageable pageable) {
    accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    
    // Return from database with pagination
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
   * Previews a changed order (modifies an existing order) using DTOs.
   * 
   * @param accountId Internal account UUID
   * @param orderId Internal order UUID
   * @param request PreviewOrderRequest DTO
   * @return PreviewOrderResponse DTO
   */
  public PreviewOrderResponse changePreviewOrder(UUID accountId, UUID orderId, PreviewOrderRequest request) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    return orderApi.changePreviewOrder(accountId, account.getAccountIdKey(), order.getEtradeOrderId(), request);
  }

  /**
   * Previews a changed order (deprecated - uses Maps).
   * @deprecated Use {@link #changePreviewOrder(UUID, UUID, PreviewOrderRequest)} instead
   */
  @Deprecated
  public Map<String, Object> changePreviewOrder(UUID accountId, UUID orderId, Map<String, Object> orderRequest) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    return orderClient.changePreviewOrder(accountId, account.getAccountIdKey(), order.getEtradeOrderId(), orderRequest);
  }

  /**
   * Places a changed order (modifies and places an existing order) using DTOs.
   * 
   * @param accountId Internal account UUID
   * @param orderId Internal order UUID
   * @param request PlaceOrderRequest DTO
   * @return EtradeOrderDto (internal order DTO)
   */
  @Transactional
  public EtradeOrderDto placeChangedOrder(UUID accountId, UUID orderId, PlaceOrderRequest request) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    // Build preview request from place request
    PreviewOrderRequest previewRequest = new PreviewOrderRequest();
    previewRequest.setOrderType(request.getOrderType());
    previewRequest.setClientOrderId(request.getClientOrderId());
    previewRequest.setOrders(request.getOrders());

    // Preview first
    PreviewOrderResponse preview = orderApi.changePreviewOrder(accountId, account.getAccountIdKey(), 
        order.getEtradeOrderId(), previewRequest);

    // Use the first preview ID from the preview response
    if (preview.getPreviewIds().isEmpty()) {
      throw new RuntimeException("No preview ID returned from preview order");
    }
    String previewId = preview.getPreviewIds().get(0).getPreviewId();

    // Update place request with preview ID
    PlaceOrderRequest placeRequest = new PlaceOrderRequest();
    placeRequest.setPreviewId(previewId);
    placeRequest.setOrderType(request.getOrderType());
    placeRequest.setClientOrderId(request.getClientOrderId());
    placeRequest.setOrders(request.getOrders());

    // Place changed order
    PlaceOrderResponse placeResponse = orderApi.placeChangedOrder(accountId, account.getAccountIdKey(), 
        order.getEtradeOrderId(), placeRequest);

    // Update order in database
    if (!placeResponse.getOrderIds().isEmpty()) {
      order.setEtradeOrderId(placeResponse.getOrderIds().get(0).getOrderId());
    }
    
    // Set placedTime from response
    if (placeResponse.getPlacedTime() != null) {
      order.setPlacedTime(placeResponse.getPlacedTime());
      order.setPlacedAt(OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(placeResponse.getPlacedTime()),
          java.time.ZoneOffset.UTC));
    } else {
      order.setPlacedAt(OffsetDateTime.now());
    }

    try {
      order.setPreviewData(objectMapper.writeValueAsString(preview));
      order.setOrderResponse(objectMapper.writeValueAsString(placeResponse));
    } catch (Exception e) {
      log.warn("Failed to serialize order data", e);
    }

    order.setOrderStatus("SUBMITTED");

    EtradeOrder saved = orderRepository.save(order);
    log.info("Placed changed E*TRADE order {} for account {}", saved.getEtradeOrderId(), accountId);

    return toDto(saved);
  }

  /**
   * Places a changed order (deprecated - uses Maps).
   * @deprecated Use {@link #placeChangedOrder(UUID, UUID, PlaceOrderRequest)} instead
   */
  @Deprecated
  @Transactional
  public EtradeOrderDto changePlaceOrder(UUID accountId, UUID orderId, Map<String, Object> orderRequest) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    // Preview first
    Map<String, Object> preview = orderClient.changePreviewOrder(accountId, account.getAccountIdKey(), 
        order.getEtradeOrderId(), orderRequest);

    // Place changed order
    Map<String, Object> placeResponse = orderClient.changePlaceOrder(accountId, account.getAccountIdKey(), 
        order.getEtradeOrderId(), orderRequest);

    // Update order in database
    @SuppressWarnings("unchecked")
    List<String> orderIds = (List<String>) placeResponse.get("orderIds");
    if (orderIds != null && !orderIds.isEmpty()) {
      order.setEtradeOrderId(orderIds.get(0));
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
    log.info("Placed changed E*TRADE order {} for account {}", saved.getEtradeOrderId(), accountId);

    return toDto(saved);
  }

  /**
   * Cancels an order using DTOs.
   * 
   * @param accountId Internal account UUID
   * @param orderId Internal order UUID
   * @return CancelOrderResponse DTO
   */
  @Transactional
  public CancelOrderResponse cancelOrder(UUID accountId, UUID orderId) {
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!order.getAccountId().equals(accountId)) {
      throw new RuntimeException("Order does not belong to account");
    }

    EtradeAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

    CancelOrderRequest request = new CancelOrderRequest(order.getEtradeOrderId());
    CancelOrderResponse response = orderApi.cancelOrder(accountId, account.getAccountIdKey(), request);

    if (Boolean.TRUE.equals(response.getSuccess())) {
      order.setOrderStatus("CANCELLED");
      order.setCancelledAt(OffsetDateTime.now());
      orderRepository.save(order);
      log.info("Cancelled E*TRADE order {}", order.getEtradeOrderId());
    } else {
      log.warn("Failed to cancel E*TRADE order {}: {}", order.getEtradeOrderId(), response.getMessages());
    }

    return response;
  }

  /**
   * Cancels an order (deprecated - returns internal order DTO).
   * @deprecated Use {@link #cancelOrder(UUID, UUID)} which returns CancelOrderResponse
   */
  @Deprecated
  @Transactional
  public EtradeOrderDto cancelOrderLegacy(UUID accountId, UUID orderId) {
    // Cancel the order (response is used to ensure cancellation happens)
    cancelOrder(accountId, orderId);
    
    EtradeOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    
    return toDto(order);
  }

  // ============================================================================
  // Persistence Helper Methods
  // ============================================================================

  /**
   * Persists orders from List Orders response (upsert by orderId + accountIdKey).
   */
  private void persistOrders(UUID accountId, String accountIdKey, OrdersResponse response) {
    try {
      List<EtradeOrderModel> orders = response.getOrders();
      if (orders == null || orders.isEmpty()) {
        log.debug("No orders to persist for account {}", accountId);
        return;
      }
      
      OffsetDateTime syncTime = OffsetDateTime.now();
      
      for (EtradeOrderModel orderModel : orders) {
        if (orderModel.getOrderId() == null || orderModel.getOrderId().isEmpty()) {
          log.warn("Order missing orderId, skipping persistence");
          continue;
        }
        
        // Upsert by orderId + accountIdKey
        Optional<EtradeOrder> existing = orderRepository.findByEtradeOrderIdAndAccountIdKey(
            orderModel.getOrderId(), accountIdKey);
        EtradeOrder order;
        
        if (existing.isPresent()) {
          // Update existing order
          order = existing.get();
          order.setLastSyncedAt(syncTime);
        } else {
          // Create new order
          order = new EtradeOrder();
          order.setAccountId(accountId);
          order.setAccountIdKey(accountIdKey);
          order.setEtradeOrderId(orderModel.getOrderId());
          order.setLastSyncedAt(syncTime);
        }
        
        // Update order fields from model
        updateOrderFromModel(order, orderModel);
        
        // Store raw response as JSON
        try {
          order.setOrderResponse(objectMapper.writeValueAsString(orderModel));
        } catch (Exception e) {
          log.warn("Failed to serialize order to JSON", e);
        }
        
        orderRepository.save(order);
        log.debug("Persisted order {} for account {}", orderModel.getOrderId(), accountId);
      }
      
      log.info("Persisted {} orders for account {}", orders.size(), accountId);
    } catch (Exception e) {
      log.error("Failed to persist orders for account {}", accountId, e);
      // Don't throw - persistence failure shouldn't break the API call
    }
  }

  /**
   * Persists preview attempt (saves preview record).
   */
  private void persistPreviewAttempt(UUID accountId, String accountIdKey, 
                                     PreviewOrderRequest request, PreviewOrderResponse response) {
    try {
      if (response.getPreviewIds().isEmpty()) {
        log.debug("No preview IDs in response, skipping preview persistence");
        return;
      }
      
      String previewId = response.getPreviewIds().get(0).getPreviewId();
      if (previewId == null || previewId.isEmpty()) {
        log.debug("Preview ID is null or empty, skipping preview persistence");
        return;
      }
      
      // Create a preview record (could be stored in a separate table, but for now we'll
      // store it as an order with preview data)
      EtradeOrder previewOrder = new EtradeOrder();
      previewOrder.setAccountId(accountId);
      previewOrder.setAccountIdKey(accountIdKey);
      previewOrder.setPreviewId(previewId);
      previewOrder.setClientOrderId(request.getClientOrderId());
      previewOrder.setPreviewTime(OffsetDateTime.now());
      previewOrder.setLastSyncedAt(OffsetDateTime.now());
      
      // Extract order details from request for preview record
      if (!request.getOrders().isEmpty()) {
        OrderDetailDto orderDetail = request.getOrders().get(0);
        previewOrder.setPriceType(orderDetail.getPriceType());
        previewOrder.setOrderType(request.getOrderType());
        
        if (orderDetail.getLimitPrice() != null) {
          previewOrder.setLimitPrice(BigDecimal.valueOf(orderDetail.getLimitPrice()));
        }
        if (orderDetail.getStopPrice() != null) {
          previewOrder.setStopPrice(BigDecimal.valueOf(orderDetail.getStopPrice()));
        }
        
        // Extract instrument details
        if (!orderDetail.getInstruments().isEmpty()) {
          OrderInstrumentDto instrument = orderDetail.getInstruments().get(0);
          if (instrument.getProduct() != null) {
            previewOrder.setSymbol(instrument.getProduct().getSymbol());
          }
          previewOrder.setQuantity(instrument.getQuantity());
          previewOrder.setSide(instrument.getOrderAction());
        }
      }
      
      // Store preview response as JSON
      try {
        previewOrder.setPreviewData(objectMapper.writeValueAsString(response));
      } catch (Exception e) {
        log.warn("Failed to serialize preview response to JSON", e);
      }
      
      // Store preview request as part of preview data
      previewOrder.setOrderStatus("PREVIEW");
      
      orderRepository.save(previewOrder);
      log.info("Persisted preview attempt {} for account {}", previewId, accountId);
    } catch (Exception e) {
      log.error("Failed to persist preview attempt for account {}", accountId, e);
      // Don't throw - persistence failure shouldn't break the API call
    }
  }

  /**
   * Updates order entity from EtradeOrderModel DTO.
   */
  private void updateOrderFromModel(EtradeOrder order, EtradeOrderModel orderModel) {
    order.setOrderType(orderModel.getOrderType());
    order.setOrderStatus(orderModel.getOrderStatus());
    order.setClientOrderId(orderModel.getClientOrderId());
    
    if (orderModel.getPlacedTime() != null) {
      order.setPlacedTime(orderModel.getPlacedTime());
      // Convert epoch milliseconds to OffsetDateTime
      order.setPlacedAt(OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(orderModel.getPlacedTime()),
          java.time.ZoneOffset.UTC));
    }
    
    // Update order details from first order detail
    if (orderModel.getOrderDetails() != null && !orderModel.getOrderDetails().isEmpty()) {
      OrderDetailDto orderDetail = orderModel.getOrderDetails().get(0);
      order.setPriceType(orderDetail.getPriceType());
      
      if (orderDetail.getLimitPrice() != null) {
        order.setLimitPrice(BigDecimal.valueOf(orderDetail.getLimitPrice()));
      }
      if (orderDetail.getStopPrice() != null) {
        order.setStopPrice(BigDecimal.valueOf(orderDetail.getStopPrice()));
      }
      
      // Update instrument details
      if (orderDetail.getInstruments() != null && !orderDetail.getInstruments().isEmpty()) {
        OrderInstrumentDto instrument = orderDetail.getInstruments().get(0);
        if (instrument.getProduct() != null) {
          order.setSymbol(instrument.getProduct().getSymbol());
        }
        order.setQuantity(instrument.getQuantity());
        order.setSide(instrument.getOrderAction());
      }
    }
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
