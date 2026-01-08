package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
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
   * Previews an order before placement.
   */
  public Map<String, Object> previewOrder(UUID accountId, String accountIdKey, Map<String, Object> orderRequest) {
    try {
      String url = properties.getOrderPreviewUrl(accountIdKey);
      String requestBody = objectMapper.writeValueAsString(orderRequest);
      
      String response = apiClient.makeRequest("POST", url, null, requestBody, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode previewNode = root.path("PreviewOrderResponse");
      
      return parseOrderPreview(previewNode);
    } catch (Exception e) {
      log.error("Failed to preview order", e);
      throw new RuntimeException("Failed to preview order", e);
    }
  }

  /**
   * Places an order.
   */
  public Map<String, Object> placeOrder(UUID accountId, String accountIdKey, Map<String, Object> orderRequest) {
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
    preview.put("accountId", previewNode.path("accountId").asText());
    
    JsonNode orderNode = previewNode.path("Order");
    if (!orderNode.isMissingNode()) {
      preview.put("order", parseOrderDetails(orderNode));
    }
    
    // Estimated commission and total
    JsonNode estimatedCommissionsNode = previewNode.path("estimatedCommission");
    if (!estimatedCommissionsNode.isMissingNode()) {
      preview.put("estimatedCommission", estimatedCommissionsNode.asDouble());
    }
    
    JsonNode estimatedTotalAmountNode = previewNode.path("estimatedTotalAmount");
    if (!estimatedTotalAmountNode.isMissingNode()) {
      preview.put("estimatedTotalAmount", estimatedTotalAmountNode.asDouble());
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
    order.put("priceType", orderNode.path("priceType").asText());
    order.put("status", orderNode.path("status").asText());
    order.put("placedTime", orderNode.path("placedTime").asLong());
    
    JsonNode orderDetailNode = orderNode.path("OrderDetail");
    if (!orderDetailNode.isMissingNode()) {
      order.putAll(parseOrderDetails(orderDetailNode));
    }
    
    return order;
  }

  private Map<String, Object> parseOrderDetails(JsonNode detailNode) {
    Map<String, Object> details = new HashMap<>();
    details.put("allOrNone", detailNode.path("allOrNone").asBoolean());
    
    JsonNode instrumentNode = detailNode.path("Instrument");
    if (!instrumentNode.isMissingNode()) {
      details.put("symbol", instrumentNode.path("Product").path("symbol").asText());
      details.put("quantity", instrumentNode.path("quantity").asInt());
      details.put("side", instrumentNode.path("orderAction").asText());
    }
    
    JsonNode limitPriceNode = detailNode.path("limitPrice");
    if (!limitPriceNode.isMissingNode()) {
      details.put("limitPrice", limitPriceNode.asDouble());
    }
    
    JsonNode stopPriceNode = detailNode.path("stopPrice");
    if (!stopPriceNode.isMissingNode()) {
      details.put("stopPrice", stopPriceNode.asDouble());
    }
    
    return details;
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
