package com.myqyl.aitradex.etrade.order;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OrderRequestBuilder.
 */
class OrderRequestBuilderTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  void buildPreviewOrderRequest_marketOrder_createsCorrectStructure() throws Exception {
    Map<String, Object> request = OrderRequestBuilder.buildPreviewOrderRequest(
        "AAPL",
        OrderRequestBuilder.OrderAction.BUY,
        10,
        OrderRequestBuilder.PriceType.MARKET,
        OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        null, // limitPrice
        null, // stopPrice
        OrderRequestBuilder.MarketSession.REGULAR,
        "TEST123");

    assertNotNull(request);
    assertTrue(request.containsKey("PreviewOrderRequest"));

    @SuppressWarnings("unchecked")
    Map<String, Object> previewRequest = (Map<String, Object>) request.get("PreviewOrderRequest");
    assertEquals("EQ", previewRequest.get("orderType"));
    assertEquals("TEST123", previewRequest.get("clientOrderId"));
    assertTrue(previewRequest.containsKey("Order"));

    // Serialize to JSON to verify structure
    String json = objectMapper.writeValueAsString(request);
    assertTrue(json.contains("AAPL"));
    assertTrue(json.contains("BUY"));
    assertTrue(json.contains("MARKET"));
    assertTrue(json.contains("GOOD_FOR_DAY"));
    assertTrue(json.contains("REGULAR"));
  }

  @Test
  void buildPreviewOrderRequest_limitOrder_includesLimitPrice() throws Exception {
    Map<String, Object> request = OrderRequestBuilder.buildPreviewOrderRequest(
        "TSLA",
        OrderRequestBuilder.OrderAction.SELL,
        5,
        OrderRequestBuilder.PriceType.LIMIT,
        OrderRequestBuilder.OrderTerm.GOOD_UNTIL_CANCEL,
        150.50, // limitPrice
        null, // stopPrice
        OrderRequestBuilder.MarketSession.REGULAR,
        null);

    @SuppressWarnings("unchecked")
    Map<String, Object> previewRequest = (Map<String, Object>) request.get("PreviewOrderRequest");
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> orders = (java.util.List<Map<String, Object>>) previewRequest.get("Order");
    Map<String, Object> order = orders.get(0);
    assertEquals("150.5", order.get("limitPrice").toString());
  }

  @Test
  void buildPreviewOrderRequest_stopOrder_includesStopPrice() throws Exception {
    Map<String, Object> request = OrderRequestBuilder.buildPreviewOrderRequest(
        "MSFT",
        OrderRequestBuilder.OrderAction.SELL_SHORT,
        3,
        OrderRequestBuilder.PriceType.STOP,
        OrderRequestBuilder.OrderTerm.IMMEDIATE_OR_CANCEL,
        null, // limitPrice
        300.25, // stopPrice
        OrderRequestBuilder.MarketSession.EXTENDED,
        null);

    @SuppressWarnings("unchecked")
    Map<String, Object> previewRequest = (Map<String, Object>) request.get("PreviewOrderRequest");
    @SuppressWarnings("unchecked")
    java.util.List<Map<String, Object>> orders = (java.util.List<Map<String, Object>>) previewRequest.get("Order");
    Map<String, Object> order = orders.get(0);
    assertEquals("300.25", order.get("stopPrice").toString());
    assertEquals("EXTENDED", order.get("marketSession"));
  }

  @Test
  void buildPreviewOrderRequest_generatesClientOrderId_whenNotProvided() {
    Map<String, Object> request = OrderRequestBuilder.buildPreviewOrderRequest(
        "GOOGL",
        OrderRequestBuilder.OrderAction.BUY,
        1,
        OrderRequestBuilder.PriceType.MARKET,
        OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        null,
        null,
        OrderRequestBuilder.MarketSession.REGULAR,
        null); // clientOrderId not provided

    @SuppressWarnings("unchecked")
    Map<String, Object> previewRequest = (Map<String, Object>) request.get("PreviewOrderRequest");
    String clientOrderId = (String) previewRequest.get("clientOrderId");
    assertNotNull(clientOrderId);
    assertFalse(clientOrderId.isEmpty());
  }

  @Test
  void buildPlaceOrderRequest_usesPreviewResponse() throws Exception {
    // Create preview request
    Map<String, Object> previewRequest = OrderRequestBuilder.buildPreviewOrderRequest(
        "AAPL",
        OrderRequestBuilder.OrderAction.BUY,
        10,
        OrderRequestBuilder.PriceType.MARKET,
        OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        null,
        null,
        OrderRequestBuilder.MarketSession.REGULAR,
        "CLIENT123");

    // Create mock preview response with PreviewIds
    Map<String, Object> previewResponse = new java.util.HashMap<>();
    java.util.List<Map<String, Object>> previewIds = new java.util.ArrayList<>();
    Map<String, Object> previewId = new java.util.HashMap<>();
    previewId.put("previewId", "PREVIEW456");
    previewIds.add(previewId);
    previewResponse.put("PreviewIds", previewIds);

    // Build place order request
    Map<String, Object> placeRequest = OrderRequestBuilder.buildPlaceOrderRequest(
        previewResponse, previewRequest);

    assertNotNull(placeRequest);
    assertTrue(placeRequest.containsKey("PlaceOrderRequest"));

    @SuppressWarnings("unchecked")
    Map<String, Object> placeOrderRequest = (Map<String, Object>) placeRequest.get("PlaceOrderRequest");
    assertEquals("PREVIEW456", placeOrderRequest.get("PreviewId"));
    assertEquals("EQ", placeOrderRequest.get("orderType"));
    assertEquals("CLIENT123", placeOrderRequest.get("clientOrderId"));
    assertTrue(placeOrderRequest.containsKey("Order"));
  }

  @Test
  void buildPlaceOrderRequest_handlesPreviewIdsArray() throws Exception {
    Map<String, Object> previewRequest = OrderRequestBuilder.buildPreviewOrderRequest(
        "TSLA", OrderRequestBuilder.OrderAction.BUY, 1,
        OrderRequestBuilder.PriceType.MARKET, OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        null, null, OrderRequestBuilder.MarketSession.REGULAR, "TEST");

    Map<String, Object> previewResponse = new java.util.HashMap<>();
    // PreviewIds as array with single element
    java.util.List<Map<String, Object>> previewIds = new java.util.ArrayList<>();
    Map<String, Object> previewId = new java.util.HashMap<>();
    previewId.put("previewId", "PREVIEW789");
    previewIds.add(previewId);
    previewResponse.put("PreviewIds", previewIds);

    Map<String, Object> placeRequest = OrderRequestBuilder.buildPlaceOrderRequest(
        previewResponse, previewRequest);

    @SuppressWarnings("unchecked")
    Map<String, Object> placeOrderRequest = (Map<String, Object>) placeRequest.get("PlaceOrderRequest");
    assertEquals("PREVIEW789", placeOrderRequest.get("PreviewId"));
  }

  @Test
  void orderActionEnum_hasCorrectValues() {
    assertEquals("BUY", OrderRequestBuilder.OrderAction.BUY.getValue());
    assertEquals("SELL", OrderRequestBuilder.OrderAction.SELL.getValue());
    assertEquals("SELL_SHORT", OrderRequestBuilder.OrderAction.SELL_SHORT.getValue());
    assertEquals("BUY_TO_COVER", OrderRequestBuilder.OrderAction.BUY_TO_COVER.getValue());
  }

  @Test
  void priceTypeEnum_hasCorrectValues() {
    assertEquals("MARKET", OrderRequestBuilder.PriceType.MARKET.getValue());
    assertEquals("LIMIT", OrderRequestBuilder.PriceType.LIMIT.getValue());
    assertEquals("STOP", OrderRequestBuilder.PriceType.STOP.getValue());
    assertEquals("STOP_LIMIT", OrderRequestBuilder.PriceType.STOP_LIMIT.getValue());
  }

  @Test
  void orderTermEnum_hasCorrectValues() {
    assertEquals("GOOD_FOR_DAY", OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY.getValue());
    assertEquals("GOOD_UNTIL_CANCEL", OrderRequestBuilder.OrderTerm.GOOD_UNTIL_CANCEL.getValue());
    assertEquals("IMMEDIATE_OR_CANCEL", OrderRequestBuilder.OrderTerm.IMMEDIATE_OR_CANCEL.getValue());
    assertEquals("FILL_OR_KILL", OrderRequestBuilder.OrderTerm.FILL_OR_KILL.getValue());
  }

  @Test
  void marketSessionEnum_hasCorrectValues() {
    assertEquals("REGULAR", OrderRequestBuilder.MarketSession.REGULAR.getValue());
    assertEquals("EXTENDED", OrderRequestBuilder.MarketSession.EXTENDED.getValue());
  }
}
