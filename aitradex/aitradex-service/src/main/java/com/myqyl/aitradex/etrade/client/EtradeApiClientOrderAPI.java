package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.orders.dto.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E*TRADE Order API Client.
 * 
 * This class refactors order-specific functionality from EtradeOrderClient
 * into a dedicated Order API layer.
 * 
 * Implements all 6 Order API endpoints as per E*TRADE Order API documentation:
 * 1. List Orders
 * 2. Preview Order
 * 3. Place Order
 * 4. Cancel Order
 * 5. Change Previewed Order
 * 6. Place Changed Order
 * 
 * All request and response objects are DTOs/Models, not Maps, as per requirements.
 */
@Component
public class EtradeApiClientOrderAPI {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClientOrderAPI.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeApiClientOrderAPI(
      EtradeApiClient apiClient,
      EtradeProperties properties,
      ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * 1. List Orders
   * 
   * Retrieves order details for a selected brokerage account based on search criteria.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request ListOrdersRequest DTO containing query parameters
   * @return OrdersResponse DTO containing list of orders
   * @throws EtradeApiException if the request fails
   */
  public OrdersResponse listOrders(UUID accountId, String accountIdKey, ListOrdersRequest request) {
    try {
      String url = properties.getOrdersUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      
      if (request.getMarker() != null && !request.getMarker().isEmpty()) {
        params.put("marker", request.getMarker());
      }
      if (request.getCount() != null && request.getCount() > 0) {
        params.put("count", String.valueOf(Math.min(request.getCount(), 100))); // Max 100
      }
      if (request.getStatus() != null && !request.getStatus().isEmpty()) {
        params.put("status", request.getStatus());
      }
      if (request.getFromDate() != null && !request.getFromDate().isEmpty()) {
        params.put("fromDate", request.getFromDate());
      }
      if (request.getToDate() != null && !request.getToDate().isEmpty()) {
        params.put("toDate", request.getToDate());
      }
      if (request.getSymbol() != null && !request.getSymbol().isEmpty()) {
        params.put("symbol", request.getSymbol());
      }
      if (request.getSecurityType() != null && !request.getSecurityType().isEmpty()) {
        params.put("securityType", request.getSecurityType());
      }
      if (request.getTransactionType() != null && !request.getTransactionType().isEmpty()) {
        params.put("transactionType", request.getTransactionType());
      }
      if (request.getMarketSession() != null && !request.getMarketSession().isEmpty()) {
        params.put("marketSession", request.getMarketSession());
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode ordersResponseNode = root.path("OrdersResponse");
      
      return parseOrdersResponse(ordersResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list orders for account {}", accountIdKey, e);
      throw new EtradeApiException(500, "LIST_ORDERS_FAILED", 
          "Failed to list orders: " + e.getMessage(), e);
    }
  }

  /**
   * 2. Preview Order
   * 
   * Submits an order request for preview before placing it.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request PreviewOrderRequest DTO
   * @return PreviewOrderResponse DTO containing preview IDs and order details
   * @throws EtradeApiException if the request fails
   */
  public PreviewOrderResponse previewOrder(UUID accountId, String accountIdKey, PreviewOrderRequest request) {
    try {
      String url = properties.getOrderPreviewUrl(accountIdKey);
      
      // Build JSON request body from DTO
      Map<String, Object> requestBody = buildPreviewOrderRequestBody(request);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      log.debug("Preview order request: {}", requestBodyJson);
      String response = apiClient.makeRequest("POST", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode previewNode = root.path("PreviewOrderResponse");
      
      if (previewNode.isMissingNode()) {
        // Check for errors
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          List<MessageDto> messages = parseMessages(messagesNode);
          String errorMsg = extractErrorMessage(messages);
          throw new EtradeApiException(400, "PREVIEW_ORDER_FAILED", 
              "Order preview failed: " + errorMsg, null);
        }
        throw new EtradeApiException(500, "PREVIEW_ORDER_FAILED", 
            "Invalid preview order response", null);
      }
      
      return parsePreviewOrderResponse(previewNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to preview order for account {}", accountIdKey, e);
      throw new EtradeApiException(500, "PREVIEW_ORDER_FAILED", 
          "Failed to preview order: " + e.getMessage(), e);
    }
  }

  /**
   * 3. Place Order
   * 
   * Submits an order after it has been successfully previewed.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request PlaceOrderRequest DTO
   * @return PlaceOrderResponse DTO containing order IDs
   * @throws EtradeApiException if the request fails
   */
  public PlaceOrderResponse placeOrder(UUID accountId, String accountIdKey, PlaceOrderRequest request) {
    try {
      String url = properties.getOrderPlaceUrl(accountIdKey);
      
      // Build JSON request body from DTO
      Map<String, Object> requestBody = buildPlaceOrderRequestBody(request);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      log.debug("Place order request: {}", requestBodyJson);
      String response = apiClient.makeRequest("POST", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode placeOrderNode = root.path("PlaceOrderResponse");
      
      if (placeOrderNode.isMissingNode()) {
        // Check for errors
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          List<MessageDto> messages = parseMessages(messagesNode);
          String errorMsg = extractErrorMessage(messages);
          throw new EtradeApiException(400, "PLACE_ORDER_FAILED", 
              "Order placement failed: " + errorMsg, null);
        }
        throw new EtradeApiException(500, "PLACE_ORDER_FAILED", 
            "Invalid place order response", null);
      }
      
      return parsePlaceOrderResponse(placeOrderNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to place order for account {}", accountIdKey, e);
      throw new EtradeApiException(500, "PLACE_ORDER_FAILED", 
          "Failed to place order: " + e.getMessage(), e);
    }
  }

  /**
   * 4. Cancel Order
   * 
   * Cancels an existing order.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param request CancelOrderRequest DTO containing order ID
   * @return CancelOrderResponse DTO
   * @throws EtradeApiException if the request fails
   */
  public CancelOrderResponse cancelOrder(UUID accountId, String accountIdKey, CancelOrderRequest request) {
    try {
      String url = properties.getOrderCancelUrl(accountIdKey);
      
      // Build JSON request body from DTO
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("orderId", request.getOrderId());
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      log.debug("Cancel order request: {}", requestBodyJson);
      String response = apiClient.makeRequest("PUT", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      
      return parseCancelOrderResponse(root);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to cancel order {} for account {}", request.getOrderId(), accountIdKey, e);
      throw new EtradeApiException(500, "CANCEL_ORDER_FAILED", 
          "Failed to cancel order: " + e.getMessage(), e);
    }
  }

  /**
   * 5. Change Previewed Order
   * 
   * Previews a modified order.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param orderId The order ID to modify
   * @param request PreviewOrderRequest DTO (same structure as preview order)
   * @return PreviewOrderResponse DTO
   * @throws EtradeApiException if the request fails
   */
  public PreviewOrderResponse changePreviewOrder(UUID accountId, String accountIdKey, String orderId,
                                                 PreviewOrderRequest request) {
    try {
      String url = properties.getOrderChangePreviewUrl(accountIdKey, orderId);
      
      // Build JSON request body from DTO
      Map<String, Object> requestBody = buildPreviewOrderRequestBody(request);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      log.debug("Change preview order request: {}", requestBodyJson);
      String response = apiClient.makeRequest("PUT", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode previewNode = root.path("PreviewOrderResponse");
      
      if (previewNode.isMissingNode()) {
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          List<MessageDto> messages = parseMessages(messagesNode);
          String errorMsg = extractErrorMessage(messages);
          throw new EtradeApiException(400, "CHANGE_PREVIEW_ORDER_FAILED", 
              "Change preview order failed: " + errorMsg, null);
        }
        throw new EtradeApiException(500, "CHANGE_PREVIEW_ORDER_FAILED", 
            "Invalid change preview order response", null);
      }
      
      return parsePreviewOrderResponse(previewNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to change preview order {} for account {}", orderId, accountIdKey, e);
      throw new EtradeApiException(500, "CHANGE_PREVIEW_ORDER_FAILED", 
          "Failed to change preview order: " + e.getMessage(), e);
    }
  }

  /**
   * 6. Place Changed Order
   * 
   * Places a modified order.
   * 
   * @param accountId Internal account UUID for authentication
   * @param accountIdKey E*TRADE account ID key
   * @param orderId The order ID to modify
   * @param request PlaceOrderRequest DTO (same structure as place order)
   * @return PlaceOrderResponse DTO
   * @throws EtradeApiException if the request fails
   */
  public PlaceOrderResponse placeChangedOrder(UUID accountId, String accountIdKey, String orderId,
                                             PlaceOrderRequest request) {
    try {
      String url = properties.getOrderChangePlaceUrl(accountIdKey, orderId);
      
      // Build JSON request body from DTO
      Map<String, Object> requestBody = buildPlaceOrderRequestBody(request);
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      log.debug("Place changed order request: {}", requestBodyJson);
      String response = apiClient.makeRequest("PUT", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode placeOrderNode = root.path("PlaceOrderResponse");
      
      if (placeOrderNode.isMissingNode()) {
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          List<MessageDto> messages = parseMessages(messagesNode);
          String errorMsg = extractErrorMessage(messages);
          throw new EtradeApiException(400, "PLACE_CHANGED_ORDER_FAILED", 
              "Place changed order failed: " + errorMsg, null);
        }
        throw new EtradeApiException(500, "PLACE_CHANGED_ORDER_FAILED", 
            "Invalid place changed order response", null);
      }
      
      return parsePlaceOrderResponse(placeOrderNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to place changed order {} for account {}", orderId, accountIdKey, e);
      throw new EtradeApiException(500, "PLACE_CHANGED_ORDER_FAILED", 
          "Failed to place changed order: " + e.getMessage(), e);
    }
  }

  // ============================================================================
  // Request Body Builders (convert DTOs to JSON structure)
  // ============================================================================

  /**
   * Builds PreviewOrderRequest JSON body from DTO.
   */
  private Map<String, Object> buildPreviewOrderRequestBody(PreviewOrderRequest request) {
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> previewRequest = new HashMap<>();
    
    previewRequest.put("orderType", request.getOrderType());
    if (request.getClientOrderId() != null && !request.getClientOrderId().isEmpty()) {
      previewRequest.put("clientOrderId", request.getClientOrderId());
    }
    
    // Build Order array from orderDetails
    List<Map<String, Object>> orderList = new ArrayList<>();
    for (OrderDetailDto orderDetail : request.getOrders()) {
      Map<String, Object> order = new HashMap<>();
      order.put("allOrNone", orderDetail.getAllOrNone() != null ? orderDetail.getAllOrNone().toString() : "false");
      order.put("priceType", orderDetail.getPriceType());
      order.put("orderTerm", orderDetail.getOrderTerm());
      order.put("marketSession", orderDetail.getMarketSession());
      
      if (orderDetail.getLimitPrice() != null) {
        order.put("limitPrice", orderDetail.getLimitPrice().toString());
      }
      if (orderDetail.getStopPrice() != null) {
        order.put("stopPrice", orderDetail.getStopPrice().toString());
      }
      if (orderDetail.getStopLimitPrice() != null) {
        order.put("stopLimitPrice", orderDetail.getStopLimitPrice().toString());
      }
      
      // Build Instrument array
      List<Map<String, Object>> instrumentList = new ArrayList<>();
      for (OrderInstrumentDto instrument : orderDetail.getInstruments()) {
        Map<String, Object> instrumentMap = new HashMap<>();
        
        // Product
        if (instrument.getProduct() != null) {
          Map<String, Object> product = new HashMap<>();
          product.put("symbol", instrument.getProduct().getSymbol());
          if (instrument.getProduct().getSecurityType() != null) {
            product.put("securityType", instrument.getProduct().getSecurityType());
          }
          if (instrument.getProduct().getSymbolDescription() != null) {
            product.put("symbolDescription", instrument.getProduct().getSymbolDescription());
          }
          if (instrument.getProduct().getCusip() != null) {
            product.put("cusip", instrument.getProduct().getCusip());
          }
          if (instrument.getProduct().getExchange() != null) {
            product.put("exchange", instrument.getProduct().getExchange());
          }
          instrumentMap.put("Product", product);
        }
        
        instrumentMap.put("orderAction", instrument.getOrderAction());
        instrumentMap.put("quantityType", instrument.getQuantityType() != null ? instrument.getQuantityType() : "QUANTITY");
        instrumentMap.put("quantity", instrument.getQuantity().toString());
        
        instrumentList.add(instrumentMap);
      }
      order.put("Instrument", instrumentList);
      orderList.add(order);
    }
    previewRequest.put("Order", orderList);
    
    requestBody.put("PreviewOrderRequest", previewRequest);
    return requestBody;
  }

  /**
   * Builds PlaceOrderRequest JSON body from DTO.
   */
  private Map<String, Object> buildPlaceOrderRequestBody(PlaceOrderRequest request) {
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> placeRequest = new HashMap<>();
    
    placeRequest.put("PreviewId", request.getPreviewId());
    placeRequest.put("orderType", request.getOrderType());
    if (request.getClientOrderId() != null && !request.getClientOrderId().isEmpty()) {
      placeRequest.put("clientOrderId", request.getClientOrderId());
    }
    
    // Build Order array from orderDetails (same structure as preview)
    List<Map<String, Object>> orderList = new ArrayList<>();
    for (OrderDetailDto orderDetail : request.getOrders()) {
      Map<String, Object> order = new HashMap<>();
      order.put("allOrNone", orderDetail.getAllOrNone() != null ? orderDetail.getAllOrNone().toString() : "false");
      order.put("priceType", orderDetail.getPriceType());
      order.put("orderTerm", orderDetail.getOrderTerm());
      order.put("marketSession", orderDetail.getMarketSession());
      
      if (orderDetail.getLimitPrice() != null) {
        order.put("limitPrice", orderDetail.getLimitPrice().toString());
      }
      if (orderDetail.getStopPrice() != null) {
        order.put("stopPrice", orderDetail.getStopPrice().toString());
      }
      if (orderDetail.getStopLimitPrice() != null) {
        order.put("stopLimitPrice", orderDetail.getStopLimitPrice().toString());
      }
      
      // Build Instrument array
      List<Map<String, Object>> instrumentList = new ArrayList<>();
      for (OrderInstrumentDto instrument : orderDetail.getInstruments()) {
        Map<String, Object> instrumentMap = new HashMap<>();
        
        // Product
        if (instrument.getProduct() != null) {
          Map<String, Object> product = new HashMap<>();
          product.put("symbol", instrument.getProduct().getSymbol());
          if (instrument.getProduct().getSecurityType() != null) {
            product.put("securityType", instrument.getProduct().getSecurityType());
          }
          instrumentMap.put("Product", product);
        }
        
        instrumentMap.put("orderAction", instrument.getOrderAction());
        instrumentMap.put("quantityType", instrument.getQuantityType() != null ? instrument.getQuantityType() : "QUANTITY");
        instrumentMap.put("quantity", instrument.getQuantity().toString());
        
        instrumentList.add(instrumentMap);
      }
      order.put("Instrument", instrumentList);
      orderList.add(order);
    }
    placeRequest.put("Order", orderList);
    
    requestBody.put("PlaceOrderRequest", placeRequest);
    return requestBody;
  }

  // ============================================================================
  // Response Parsers (convert JSON to DTOs)
  // ============================================================================

  /**
   * Parses OrdersResponse JSON node into OrdersResponse DTO.
   */
  private OrdersResponse parseOrdersResponse(JsonNode ordersResponseNode) {
    OrdersResponse response = new OrdersResponse();
    
    // Parse marker
    JsonNode markerNode = ordersResponseNode.path("marker");
    if (!markerNode.isMissingNode()) {
      response.setMarker(markerNode.asText());
    }
    
    // Parse moreOrders
    response.setMoreOrders(ordersResponseNode.path("moreOrders").asBoolean(false));
    
    // Parse Order array
    JsonNode orderArray = ordersResponseNode.path("Order");
    List<EtradeOrderModel> orders = new ArrayList<>();
    if (orderArray.isArray()) {
      for (JsonNode orderNode : orderArray) {
        orders.add(parseOrder(orderNode));
      }
    } else if (orderArray.isObject() && !orderArray.isMissingNode()) {
      orders.add(parseOrder(orderArray));
    }
    response.setOrders(orders);
    
    return response;
  }

  /**
   * Parses Order JSON node into EtradeOrderModel DTO.
   */
  private EtradeOrderModel parseOrder(JsonNode orderNode) {
    EtradeOrderModel order = new EtradeOrderModel();
    
    order.setOrderId(getStringValue(orderNode, "orderId"));
    order.setOrderType(getStringValue(orderNode, "orderType"));
    order.setOrderStatus(getStringValue(orderNode, "orderStatus"));
    order.setAccountId(getStringValue(orderNode, "accountId"));
    order.setClientOrderId(getStringValue(orderNode, "clientOrderId"));
    order.setPlacedTime(getLongValue(orderNode, "placedTime"));
    order.setMarker(getStringValue(orderNode, "marker"));
    
    // Parse OrderDetail array
    JsonNode orderDetailArray = orderNode.path("OrderDetail");
    if (!orderDetailArray.isMissingNode()) {
      List<OrderDetailDto> orderDetails = new ArrayList<>();
      if (orderDetailArray.isArray()) {
        for (JsonNode detailNode : orderDetailArray) {
          orderDetails.add(parseOrderDetail(detailNode));
        }
      } else {
        orderDetails.add(parseOrderDetail(orderDetailArray));
      }
      order.setOrderDetails(orderDetails);
    }
    
    return order;
  }

  /**
   * Parses OrderDetail JSON node into OrderDetailDto DTO.
   */
  private OrderDetailDto parseOrderDetail(JsonNode detailNode) {
    OrderDetailDto detail = new OrderDetailDto();
    
    detail.setAllOrNone(detailNode.path("allOrNone").asBoolean(false));
    detail.setPriceType(getStringValue(detailNode, "priceType"));
    detail.setOrderTerm(getStringValue(detailNode, "orderTerm"));
    detail.setMarketSession(getStringValue(detailNode, "marketSession"));
    detail.setLimitPrice(getDoubleValue(detailNode, "limitPrice"));
    detail.setStopPrice(getDoubleValue(detailNode, "stopPrice"));
    detail.setStopLimitPrice(getDoubleValue(detailNode, "stopLimitPrice"));
    detail.setEstimatedCommission(getDoubleValue(detailNode, "estimatedCommission"));
    detail.setEstimatedTotalAmount(getDoubleValue(detailNode, "estimatedTotalAmount"));
    detail.setOrderValue(getDoubleValue(detailNode, "orderValue"));
    
    // Parse Instrument array
    JsonNode instrumentArray = detailNode.path("Instrument");
    if (!instrumentArray.isMissingNode()) {
      List<OrderInstrumentDto> instruments = new ArrayList<>();
      if (instrumentArray.isArray()) {
        for (JsonNode instrumentNode : instrumentArray) {
          instruments.add(parseInstrument(instrumentNode));
        }
      } else {
        instruments.add(parseInstrument(instrumentArray));
      }
      detail.setInstruments(instruments);
    }
    
    return detail;
  }

  /**
   * Parses Instrument JSON node into OrderInstrumentDto DTO.
   */
  private OrderInstrumentDto parseInstrument(JsonNode instrumentNode) {
    OrderInstrumentDto instrument = new OrderInstrumentDto();
    
    instrument.setOrderAction(getStringValue(instrumentNode, "orderAction"));
    instrument.setQuantity(getIntValue(instrumentNode, "quantity"));
    instrument.setQuantityType(getStringValue(instrumentNode, "quantityType"));
    instrument.setAverageExecutionPrice(getDoubleValue(instrumentNode, "averageExecutionPrice"));
    instrument.setReservedQuantity(getIntValue(instrumentNode, "reservedQuantity"));
    instrument.setFilledQuantity(getIntValue(instrumentNode, "filledQuantity"));
    instrument.setRemainingQuantity(getIntValue(instrumentNode, "remainingQuantity"));
    
    // Parse Product
    JsonNode productNode = instrumentNode.path("Product");
    if (!productNode.isMissingNode()) {
      OrderProductDto product = new OrderProductDto();
      product.setSymbol(getStringValue(productNode, "symbol"));
      product.setSecurityType(getStringValue(productNode, "securityType"));
      product.setSymbolDescription(getStringValue(productNode, "symbolDescription"));
      product.setCusip(getStringValue(productNode, "cusip"));
      product.setExchange(getStringValue(productNode, "exchange"));
      instrument.setProduct(product);
    }
    
    return instrument;
  }

  /**
   * Parses PreviewOrderResponse JSON node into PreviewOrderResponse DTO.
   */
  private PreviewOrderResponse parsePreviewOrderResponse(JsonNode previewNode) {
    PreviewOrderResponse response = new PreviewOrderResponse();
    
    // Parse accountId (may be string or number)
    JsonNode accountIdNode = previewNode.path("accountId");
    if (!accountIdNode.isMissingNode()) {
      if (accountIdNode.isTextual()) {
        response.setAccountId(accountIdNode.asText());
      } else {
        response.setAccountId(String.valueOf(accountIdNode.asLong()));
      }
    }
    
    // Parse PreviewIds array
    JsonNode previewIdsNode = previewNode.path("PreviewIds");
    if (!previewIdsNode.isMissingNode()) {
      List<PreviewIdDto> previewIds = new ArrayList<>();
      if (previewIdsNode.isArray()) {
        for (JsonNode idNode : previewIdsNode) {
          PreviewIdDto previewId = new PreviewIdDto();
          previewId.setPreviewId(getStringValue(idNode, "previewId"));
          previewIds.add(previewId);
        }
      } else {
        PreviewIdDto previewId = new PreviewIdDto();
        previewId.setPreviewId(getStringValue(previewIdsNode, "previewId"));
        previewIds.add(previewId);
      }
      response.setPreviewIds(previewIds);
    }
    
    // Parse Order array
    JsonNode orderArray = previewNode.path("Order");
    if (!orderArray.isMissingNode()) {
      List<EtradeOrderModel> orders = new ArrayList<>();
      if (orderArray.isArray()) {
        for (JsonNode orderNode : orderArray) {
          orders.add(parseOrder(orderNode));
        }
      } else {
        orders.add(parseOrder(orderArray));
      }
      response.setOrders(orders);
    }
    
    // Parse cost estimates
    response.setTotalOrderValue(getDoubleValue(previewNode, "totalOrderValue"));
    response.setEstimatedCommission(getDoubleValue(previewNode, "estimatedCommission"));
    response.setEstimatedTotalAmount(getDoubleValue(previewNode, "estimatedTotalAmount"));
    
    // Parse Messages
    JsonNode messagesNode = previewNode.path("Messages");
    if (!messagesNode.isMissingNode()) {
      response.setMessages(parseMessages(messagesNode));
    }
    
    return response;
  }

  /**
   * Parses PlaceOrderResponse JSON node into PlaceOrderResponse DTO.
   */
  private PlaceOrderResponse parsePlaceOrderResponse(JsonNode placeOrderNode) {
    PlaceOrderResponse response = new PlaceOrderResponse();
    
    // Parse OrderIds array
    JsonNode orderIdsNode = placeOrderNode.path("OrderIds");
    if (!orderIdsNode.isMissingNode()) {
      List<OrderIdDto> orderIds = new ArrayList<>();
      if (orderIdsNode.isArray()) {
        for (JsonNode idNode : orderIdsNode) {
          OrderIdDto orderId = new OrderIdDto();
          orderId.setOrderId(getStringValue(idNode, "orderId"));
          orderIds.add(orderId);
        }
      } else {
        OrderIdDto orderId = new OrderIdDto();
        orderId.setOrderId(getStringValue(orderIdsNode, "orderId"));
        orderIds.add(orderId);
      }
      response.setOrderIds(orderIds);
    }
    
    // Parse Messages
    JsonNode messagesNode = placeOrderNode.path("Messages");
    if (!messagesNode.isMissingNode()) {
      response.setMessages(parseMessages(messagesNode));
    }
    
    return response;
  }

  /**
   * Parses CancelOrderResponse JSON node into CancelOrderResponse DTO.
   */
  private CancelOrderResponse parseCancelOrderResponse(JsonNode root) {
    CancelOrderResponse response = new CancelOrderResponse();
    response.setSuccess(true);
    
    // Parse Messages
    JsonNode messagesNode = root.path("Messages");
    if (!messagesNode.isMissingNode()) {
      response.setMessages(parseMessages(messagesNode));
      // Check if there are error messages
      for (MessageDto message : response.getMessages()) {
        if ("ERROR".equalsIgnoreCase(message.getType())) {
          response.setSuccess(false);
          break;
        }
      }
    }
    
    return response;
  }

  /**
   * Parses Messages JSON node into List of MessageDto.
   */
  private List<MessageDto> parseMessages(JsonNode messagesNode) {
    List<MessageDto> messages = new ArrayList<>();
    if (messagesNode.isArray()) {
      for (JsonNode msgNode : messagesNode) {
        MessageDto message = new MessageDto();
        message.setType(getStringValue(msgNode, "type"));
        message.setCode(getStringValue(msgNode, "code"));
        message.setDescription(getStringValue(msgNode, "description"));
        messages.add(message);
      }
    } else {
      MessageDto message = new MessageDto();
      message.setType(getStringValue(messagesNode, "type"));
      message.setCode(getStringValue(messagesNode, "code"));
      message.setDescription(getStringValue(messagesNode, "description"));
      messages.add(message);
    }
    return messages;
  }

  /**
   * Extracts error message from messages list.
   */
  private String extractErrorMessage(List<MessageDto> messages) {
    StringBuilder errorMsg = new StringBuilder();
    for (MessageDto message : messages) {
      if (message.getDescription() != null && !message.getDescription().isEmpty()) {
        if (errorMsg.length() > 0) {
          errorMsg.append("; ");
        }
        errorMsg.append(message.getDescription());
      }
    }
    return errorMsg.length() > 0 ? errorMsg.toString() : "Unknown error";
  }

  // ============================================================================
  // Helper methods for parsing JSON values
  // ============================================================================

  private String getStringValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    String text = fieldNode.asText();
    return (text != null && !text.isEmpty()) ? text : null;
  }

  private Double getDoubleValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asDouble();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer getIntValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asInt();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Long getLongValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asLong();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
