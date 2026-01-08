package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.order.OrderRequestBuilder;
import java.util.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for E*TRADE Order API endpoints.
 */
@Component
public class EtradeOrderClient {

  private static final Logger log = LoggerFactory.getLogger(EtradeOrderClient.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeOrderClient(EtradeApiClient apiClient, EtradeProperties properties, 
                          ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * Previews an order before placement using typed parameters.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param symbol Stock symbol
   * @param action Order action (BUY, SELL, etc.)
   * @param quantity Order quantity
   * @param priceType Price type (MARKET, LIMIT, etc.)
   * @param orderTerm Order term (GOOD_FOR_DAY, etc.)
   * @param limitPrice Limit price (required for LIMIT orders)
   * @param stopPrice Stop price (required for STOP orders)
   * @param marketSession Market session (default: REGULAR)
   * @param clientOrderId Optional client-generated order ID
   * @return Preview order response
   */
  public Map<String, Object> previewOrder(UUID accountId, String accountIdKey,
      String symbol, OrderRequestBuilder.OrderAction action, Integer quantity,
      OrderRequestBuilder.PriceType priceType, OrderRequestBuilder.OrderTerm orderTerm,
      Double limitPrice, Double stopPrice, OrderRequestBuilder.MarketSession marketSession,
      String clientOrderId) {
    try {
      // Build order request using OrderRequestBuilder
      Map<String, Object> orderRequest = OrderRequestBuilder.buildPreviewOrderRequest(
          symbol, action, quantity, priceType, orderTerm, limitPrice, stopPrice,
          marketSession != null ? marketSession : OrderRequestBuilder.MarketSession.REGULAR,
          clientOrderId);

      String url = properties.getOrderPreviewUrl(accountIdKey);
      String requestBody = objectMapper.writeValueAsString(orderRequest);
      
      log.debug("Order preview request: {}", requestBody);
      String response = apiClient.makeRequest("POST", url, null, requestBody, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode previewNode = root.path("PreviewOrderResponse");
      
      if (previewNode.isMissingNode()) {
        // Check for errors
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          String errorMsg = extractErrorMessage(messagesNode);
          throw new RuntimeException("Order preview failed: " + errorMsg);
        }
        throw new RuntimeException("Invalid preview order response");
      }
      
      return parseOrderPreview(previewNode);
    } catch (Exception e) {
      log.error("Failed to preview order", e);
      throw new RuntimeException("Failed to preview order", e);
    }
  }

  /**
   * Previews an order using a map (for backward compatibility).
   * @deprecated Use the typed method instead
   */
  @Deprecated
  public Map<String, Object> previewOrder(UUID accountId, String accountIdKey, Map<String, Object> orderRequestMap) {
    // Convert map to typed parameters
    String symbol = (String) orderRequestMap.get("symbol");
    OrderRequestBuilder.OrderAction action = parseOrderAction((String) orderRequestMap.get("action"));
    Integer quantity = Integer.valueOf(orderRequestMap.get("quantity").toString());
    OrderRequestBuilder.PriceType priceType = parsePriceType((String) orderRequestMap.get("priceType"));
    OrderRequestBuilder.OrderTerm orderTerm = parseOrderTerm((String) orderRequestMap.get("orderTerm"));
    Double limitPrice = orderRequestMap.containsKey("limitPrice") ? 
        Double.valueOf(orderRequestMap.get("limitPrice").toString()) : null;
    Double stopPrice = orderRequestMap.containsKey("stopPrice") ? 
        Double.valueOf(orderRequestMap.get("stopPrice").toString()) : null;
    String clientOrderId = (String) orderRequestMap.get("clientOrderId");

    return previewOrder(accountId, accountIdKey, symbol, action, quantity, priceType, orderTerm,
        limitPrice, stopPrice, OrderRequestBuilder.MarketSession.REGULAR, clientOrderId);
  }

  /**
   * Places an order using preview response.
   * 
   * @param accountId Internal account UUID
   * @param accountIdKey E*TRADE account ID key
   * @param previewResponse The preview order response containing previewId
   * @param previewRequest The original preview request
   * @return Place order response
   */
  public Map<String, Object> placeOrder(UUID accountId, String accountIdKey,
      Map<String, Object> previewResponse, Map<String, Object> previewRequest) {
    try {
      // Build place order request using preview response
      Map<String, Object> placeOrderRequest = OrderRequestBuilder.buildPlaceOrderRequest(
          previewResponse, previewRequest);

      String url = properties.getOrderPlaceUrl(accountIdKey);
      String requestBody = objectMapper.writeValueAsString(placeOrderRequest);
      
      log.debug("Place order request: {}", requestBody);
      String response = apiClient.makeRequest("POST", url, null, requestBody, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode orderNode = root.path("PlaceOrderResponse");
      
      if (orderNode.isMissingNode()) {
        // Check for errors
        JsonNode messagesNode = root.path("Messages");
        if (!messagesNode.isMissingNode()) {
          String errorMsg = extractErrorMessage(messagesNode);
          throw new RuntimeException("Order placement failed: " + errorMsg);
        }
        throw new RuntimeException("Invalid place order response");
      }
      
      return parsePlaceOrderResponse(orderNode);
    } catch (Exception e) {
      log.error("Failed to place order", e);
      throw new RuntimeException("Failed to place order", e);
    }
  }

  /**
   * Places an order using a map (for backward compatibility).
   * @deprecated Use the typed method instead
   */
  @Deprecated
  public Map<String, Object> placeOrder(UUID accountId, String accountIdKey, Map<String, Object> orderRequest) {
    // For backward compatibility, assume orderRequest already has correct structure
    try {
      String url = properties.getOrderPlaceUrl(accountIdKey);
      String requestBody = objectMapper.writeValueAsString(orderRequest);
      
      String response = apiClient.makeRequest("POST", url, null, requestBody, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode orderNode = root.path("PlaceOrderResponse");
      
      return parsePlaceOrderResponse(orderNode);
    } catch (Exception e) {
      log.error("Failed to place order", e);
      throw new RuntimeException("Failed to place order", e);
    }
  }

  /**
   * Gets list of orders for an account.
   */
  public List<Map<String, Object>> getOrders(UUID accountId, String accountIdKey) {
    try {
      String url = properties.getOrdersUrl(accountIdKey);
      Map<String, String> params = new HashMap<>();
      params.put("fromDate", ""); // Can specify date range
      params.put("toDate", "");
      params.put("status", "ALL");
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode ordersNode = root.path("OrdersResponse").path("Order");
      
      List<Map<String, Object>> orders = new ArrayList<>();
      if (ordersNode.isArray()) {
        for (JsonNode orderNode : ordersNode) {
          orders.add(parseOrder(orderNode));
        }
      } else if (ordersNode.isObject()) {
        orders.add(parseOrder(ordersNode));
      }
      
      return orders;
    } catch (Exception e) {
      log.error("Failed to get orders for account {}", accountIdKey, e);
      throw new RuntimeException("Failed to get orders", e);
    }
  }

  /**
   * Gets order details.
   */
  public Map<String, Object> getOrderDetails(UUID accountId, String accountIdKey, String orderId) {
    try {
      String url = properties.getOrdersUrl(accountIdKey) + "/" + orderId;
      String response = apiClient.makeRequest("GET", url, null, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode orderNode = root.path("OrdersResponse").path("Order");
      
      return parseOrder(orderNode);
    } catch (Exception e) {
      log.error("Failed to get order details for order {}", orderId, e);
      throw new RuntimeException("Failed to get order details", e);
    }
  }

  /**
   * Cancels an order.
   */
  public Map<String, Object> cancelOrder(UUID accountId, String accountIdKey, String orderId) {
    try {
      String url = properties.getOrderCancelUrl(accountIdKey);
      Map<String, Object> cancelRequest = new HashMap<>();
      cancelRequest.put("orderId", orderId);
      String requestBody = objectMapper.writeValueAsString(cancelRequest);
      
      String response = apiClient.makeRequest("PUT", url, null, requestBody, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      return parseCancelOrderResponse(root);
    } catch (Exception e) {
      log.error("Failed to cancel order {}", orderId, e);
      throw new RuntimeException("Failed to cancel order", e);
    }
  }

  private Map<String, Object> parseOrderPreview(JsonNode previewNode) {
    Map<String, Object> preview = new HashMap<>();
    
    // Account ID
    JsonNode accountIdNode = previewNode.path("accountId");
    if (!accountIdNode.isMissingNode()) {
      if (accountIdNode.isTextual()) {
        preview.put("accountId", accountIdNode.asText());
      } else {
        preview.put("accountId", accountIdNode.asLong());
      }
    }
    
    // Handle PreviewIds array
    JsonNode previewIdsNode = previewNode.path("PreviewIds");
    if (!previewIdsNode.isMissingNode()) {
      List<Map<String, Object>> previewIds = new ArrayList<>();
      if (previewIdsNode.isArray()) {
        for (JsonNode idNode : previewIdsNode) {
          Map<String, Object> previewId = new HashMap<>();
          previewId.put("previewId", idNode.path("previewId").asText(""));
          previewIds.add(previewId);
        }
      } else {
        Map<String, Object> previewId = new HashMap<>();
        previewId.put("previewId", previewIdsNode.path("previewId").asText(""));
        previewIds.add(previewId);
      }
      preview.put("PreviewIds", previewIds);
    }
    
    // Handle Order array
    JsonNode orderNode = previewNode.path("Order");
    if (!orderNode.isMissingNode()) {
      List<Map<String, Object>> orders = new ArrayList<>();
      if (orderNode.isArray()) {
        for (JsonNode order : orderNode) {
          orders.add(parseOrderDetails(order));
        }
      } else {
        orders.add(parseOrderDetails(orderNode));
      }
      preview.put("order", orders.isEmpty() ? null : orders.get(0)); // Use first order
      preview.put("orders", orders); // All orders
    }
    
    // Total order value
    JsonNode totalOrderValueNode = previewNode.path("totalOrderValue");
    if (!totalOrderValueNode.isMissingNode()) {
      if (totalOrderValueNode.isNumber()) {
        preview.put("totalOrderValue", totalOrderValueNode.asDouble());
      } else {
        preview.put("totalOrderValue", totalOrderValueNode.asText());
      }
    }
    
    // Estimated commission and total
    JsonNode estimatedCommissionsNode = previewNode.path("estimatedCommission");
    if (!estimatedCommissionsNode.isMissingNode()) {
      if (estimatedCommissionsNode.isNumber()) {
        preview.put("estimatedCommission", estimatedCommissionsNode.asDouble());
      } else {
        preview.put("estimatedCommission", estimatedCommissionsNode.asText());
      }
    }
    
    JsonNode estimatedTotalAmountNode = previewNode.path("estimatedTotalAmount");
    if (!estimatedTotalAmountNode.isMissingNode()) {
      if (estimatedTotalAmountNode.isNumber()) {
        preview.put("estimatedTotalAmount", estimatedTotalAmountNode.asDouble());
      } else {
        preview.put("estimatedTotalAmount", estimatedTotalAmountNode.asText());
      }
    }
    
    return preview;
  }

  private Map<String, Object> parsePlaceOrderResponse(JsonNode orderNode) {
    Map<String, Object> result = new HashMap<>();
    
    JsonNode orderIdsNode = orderNode.path("OrderIds");
    if (!orderIdsNode.isMissingNode() && orderIdsNode.isArray()) {
      List<String> orderIds = new ArrayList<>();
      for (JsonNode idNode : orderIdsNode) {
        orderIds.add(idNode.path("orderId").asText());
      }
      result.put("orderIds", orderIds);
    }
    
    JsonNode messagesNode = orderNode.path("Messages");
    if (!messagesNode.isMissingNode() && messagesNode.isArray()) {
      List<Map<String, Object>> messages = new ArrayList<>();
      for (JsonNode msgNode : messagesNode) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", msgNode.path("type").asText());
        msg.put("code", msgNode.path("code").asText());
        msg.put("description", msgNode.path("description").asText());
        messages.add(msg);
      }
      result.put("messages", messages);
    }
    
    return result;
  }

  private Map<String, Object> parseOrder(JsonNode orderNode) {
    Map<String, Object> order = new HashMap<>();
    order.put("orderId", orderNode.path("orderId").asText());
    order.put("orderType", orderNode.path("orderType").asText());
    order.put("orderStatus", orderNode.path("orderStatus").asText());
    
    // Handle OrderDetail array (may have multiple order details)
    JsonNode orderDetailNode = orderNode.path("OrderDetail");
    if (!orderDetailNode.isMissingNode()) {
      List<Map<String, Object>> orderDetails = new ArrayList<>();
      
      if (orderDetailNode.isArray()) {
        // Multiple order details
        for (JsonNode detailNode : orderDetailNode) {
          orderDetails.add(parseOrderDetails(detailNode));
        }
        // For single order, use first detail's properties directly
        if (!orderDetails.isEmpty()) {
          order.putAll(orderDetails.get(0));
        }
      } else {
        // Single order detail
        order.putAll(parseOrderDetails(orderDetailNode));
      }
    }
    
    // Handle placedTime (may be long or string)
    JsonNode placedTimeNode = orderNode.path("placedTime");
    if (!placedTimeNode.isMissingNode()) {
      if (placedTimeNode.isLong()) {
        order.put("placedTime", placedTimeNode.asLong());
      } else {
        order.put("placedTime", placedTimeNode.asText());
      }
    }
    
    return order;
  }

  private Map<String, Object> parseOrderDetails(JsonNode detailNode) {
    Map<String, Object> details = new HashMap<>();
    
    // Basic order detail fields
    JsonNode allOrNoneNode = detailNode.path("allOrNone");
    if (!allOrNoneNode.isMissingNode()) {
      details.put("allOrNone", allOrNoneNode.asBoolean());
    }
    
    details.put("priceType", detailNode.path("priceType").asText(""));
    details.put("orderTerm", detailNode.path("orderTerm").asText(""));
    details.put("marketSession", detailNode.path("marketSession").asText(""));
    
    // Handle limit/stop prices
    JsonNode limitPriceNode = detailNode.path("limitPrice");
    if (!limitPriceNode.isMissingNode() && !limitPriceNode.isNull()) {
      if (limitPriceNode.isNumber()) {
        details.put("limitPrice", limitPriceNode.asDouble());
      } else {
        details.put("limitPrice", limitPriceNode.asText());
      }
    }
    
    JsonNode stopPriceNode = detailNode.path("stopPrice");
    if (!stopPriceNode.isMissingNode() && !stopPriceNode.isNull()) {
      if (stopPriceNode.isNumber()) {
        details.put("stopPrice", stopPriceNode.asDouble());
      } else {
        details.put("stopPrice", stopPriceNode.asText());
      }
    }
    
    // Handle Instrument array (may have multiple instruments)
    JsonNode instrumentNode = detailNode.path("Instrument");
    if (!instrumentNode.isMissingNode()) {
      List<Map<String, Object>> instruments = new ArrayList<>();
      
      if (instrumentNode.isArray()) {
        // Multiple instruments
        for (JsonNode instNode : instrumentNode) {
          instruments.add(parseInstrument(instNode));
        }
        // For single order detail, merge first instrument's properties
        if (!instruments.isEmpty()) {
          details.putAll(instruments.get(0));
        }
        details.put("instruments", instruments);
      } else {
        // Single instrument
        Map<String, Object> instrument = parseInstrument(instrumentNode);
        details.putAll(instrument);
      }
    }
    
    return details;
  }

  private Map<String, Object> parseInstrument(JsonNode instrumentNode) {
    Map<String, Object> instrument = new HashMap<>();
    
    // Product information
    JsonNode productNode = instrumentNode.path("Product");
    if (!productNode.isMissingNode()) {
      instrument.put("symbol", productNode.path("symbol").asText(""));
      instrument.put("securityType", productNode.path("securityType").asText(""));
      instrument.put("symbolDescription", productNode.path("symbolDescription").asText(""));
    }
    
    instrument.put("orderAction", instrumentNode.path("orderAction").asText(""));
    instrument.put("quantity", instrumentNode.path("quantity").asInt(0));
    instrument.put("quantityType", instrumentNode.path("quantityType").asText("QUANTITY"));
    
    // Average execution price (if executed)
    JsonNode avgExecPriceNode = instrumentNode.path("averageExecutionPrice");
    if (!avgExecPriceNode.isMissingNode() && !avgExecPriceNode.isNull()) {
      if (avgExecPriceNode.isNumber()) {
        instrument.put("averageExecutionPrice", avgExecPriceNode.asDouble());
      } else {
        instrument.put("averageExecutionPrice", avgExecPriceNode.asText());
      }
    }
    
    return instrument;
  }

  private String extractErrorMessage(JsonNode messagesNode) {
    StringBuilder errorMsg = new StringBuilder();
    if (messagesNode.isArray()) {
      for (JsonNode msgNode : messagesNode) {
        String description = msgNode.path("description").asText("");
        if (!description.isEmpty()) {
          if (errorMsg.length() > 0) {
            errorMsg.append("; ");
          }
          errorMsg.append(description);
        }
      }
    } else {
      errorMsg.append(messagesNode.path("description").asText("Unknown error"));
    }
    return errorMsg.length() > 0 ? errorMsg.toString() : "Unknown error";
  }

  // Helper methods for parsing enums from strings
  private OrderRequestBuilder.OrderAction parseOrderAction(String action) {
    if (action == null) return OrderRequestBuilder.OrderAction.BUY;
    try {
      return OrderRequestBuilder.OrderAction.valueOf(action.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown order action: {}, defaulting to BUY", action);
      return OrderRequestBuilder.OrderAction.BUY;
    }
  }

  private OrderRequestBuilder.PriceType parsePriceType(String priceType) {
    if (priceType == null) return OrderRequestBuilder.PriceType.MARKET;
    try {
      return OrderRequestBuilder.PriceType.valueOf(priceType.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown price type: {}, defaulting to MARKET", priceType);
      return OrderRequestBuilder.PriceType.MARKET;
    }
  }

  private OrderRequestBuilder.OrderTerm parseOrderTerm(String orderTerm) {
    if (orderTerm == null) return OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY;
    try {
      return OrderRequestBuilder.OrderTerm.valueOf(orderTerm.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown order term: {}, defaulting to GOOD_FOR_DAY", orderTerm);
      return OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY;
    }
  }

  private Map<String, Object> parseCancelOrderResponse(JsonNode root) {
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    
    JsonNode messagesNode = root.path("Messages");
    if (!messagesNode.isMissingNode() && messagesNode.isArray()) {
      List<Map<String, Object>> messages = new ArrayList<>();
      for (JsonNode msgNode : messagesNode) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", msgNode.path("type").asText());
        msg.put("code", msgNode.path("code").asText());
        msg.put("description", msgNode.path("description").asText());
        messages.add(msg);
      }
      result.put("messages", messages);
    }
    
    return result;
  }
}
