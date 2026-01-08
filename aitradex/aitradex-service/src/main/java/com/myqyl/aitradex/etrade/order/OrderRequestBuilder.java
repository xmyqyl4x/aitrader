package com.myqyl.aitradex.etrade.order;

import java.util.*;
import java.util.UUID;

/**
 * Builder for E*TRADE order preview and place order requests.
 * Replaces Velocity template from example app with programmatic JSON building.
 */
public class OrderRequestBuilder {

  public enum OrderType {
    EQ("EQ"), // Equity
    OPT("OPT"), // Option
    MUTUAL_FUND("MF"); // Mutual Fund

    private final String value;

    OrderType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum OrderAction {
    BUY("BUY"),
    SELL("SELL"),
    SELL_SHORT("SELL_SHORT"),
    BUY_TO_COVER("BUY_TO_COVER");

    private final String value;

    OrderAction(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum PriceType {
    MARKET("MARKET"),
    LIMIT("LIMIT"),
    STOP("STOP"),
    STOP_LIMIT("STOP_LIMIT");

    private final String value;

    PriceType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum OrderTerm {
    GOOD_FOR_DAY("GOOD_FOR_DAY"),
    GOOD_UNTIL_CANCEL("GOOD_UNTIL_CANCEL"),
    IMMEDIATE_OR_CANCEL("IMMEDIATE_OR_CANCEL"),
    FILL_OR_KILL("FILL_OR_KILL");

    private final String value;

    OrderTerm(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum MarketSession {
    REGULAR("REGULAR"),
    EXTENDED("EXTENDED");

    private final String value;

    MarketSession(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Builds a preview order request body matching the Velocity template structure.
   *
   * @param symbol Stock symbol
   * @param action Order action (BUY, SELL, etc.)
   * @param quantity Order quantity
   * @param priceType Price type (MARKET, LIMIT, etc.)
   * @param orderTerm Order term (GOOD_FOR_DAY, etc.)
   * @param limitPrice Limit price (required for LIMIT orders)
   * @param stopPrice Stop price (required for STOP orders)
   * @param marketSession Market session (REGULAR, EXTENDED)
   * @param clientOrderId Optional client-generated order ID
   * @return Map structure ready for JSON serialization
   */
  public static Map<String, Object> buildPreviewOrderRequest(
      String symbol,
      OrderAction action,
      Integer quantity,
      PriceType priceType,
      OrderTerm orderTerm,
      Double limitPrice,
      Double stopPrice,
      MarketSession marketSession,
      String clientOrderId) {

    Map<String, Object> request = new LinkedHashMap<>();
    Map<String, Object> previewRequest = new LinkedHashMap<>();

    // Order type - default to EQ (Equity)
    previewRequest.put("orderType", OrderType.EQ.getValue());

    // Client order ID - generate if not provided
    if (clientOrderId == null || clientOrderId.isEmpty()) {
      clientOrderId = UUID.randomUUID().toString().substring(0, 8);
    }
    previewRequest.put("clientOrderId", clientOrderId);

    // Build Order array (single order)
    List<Map<String, Object>> orderList = new ArrayList<>();
    Map<String, Object> order = new LinkedHashMap<>();

    order.put("allOrNone", "false");
    order.put("priceType", priceType.getValue());
    order.put("orderTerm", orderTerm.getValue());
    order.put("marketSession", marketSession != null ? marketSession.getValue() : MarketSession.REGULAR.getValue());

    // Optional prices
    if (stopPrice != null && stopPrice > 0) {
      order.put("stopPrice", stopPrice.toString());
    }
    if (limitPrice != null && limitPrice > 0) {
      order.put("limitPrice", limitPrice.toString());
    }

    // Build Instrument array
    List<Map<String, Object>> instrumentList = new ArrayList<>();
    Map<String, Object> instrument = new LinkedHashMap<>();

    // Product information
    Map<String, Object> product = new LinkedHashMap<>();
    product.put("securityType", OrderType.EQ.getValue()); // Default to EQ
    product.put("symbol", symbol);
    instrument.put("Product", product);

    instrument.put("orderAction", action.getValue());
    instrument.put("quantityType", "QUANTITY");
    instrument.put("quantity", quantity.toString());

    instrumentList.add(instrument);
    order.put("Instrument", instrumentList);

    orderList.add(order);
    previewRequest.put("Order", orderList);

    request.put("PreviewOrderRequest", previewRequest);

    return request;
  }

  /**
   * Builds a place order request body (uses same structure as preview).
   *
   * @param previewResponse The preview order response containing previewId
   * @param previewRequest The original preview request
   * @return Map structure ready for JSON serialization
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> buildPlaceOrderRequest(
      Map<String, Object> previewResponse,
      Map<String, Object> previewRequest) {

    Map<String, Object> request = new LinkedHashMap<>();
    Map<String, Object> placeRequest = new LinkedHashMap<>();

    // Copy orderType and clientOrderId from preview
    Map<String, Object> previewReq = (Map<String, Object>) previewRequest.get("PreviewOrderRequest");
    if (previewReq != null) {
      placeRequest.put("orderType", previewReq.get("orderType"));
      placeRequest.put("clientOrderId", previewReq.get("clientOrderId"));
    }

    // Get previewId from preview response
    Object previewIds = previewResponse.get("PreviewIds");
    if (previewIds != null) {
      if (previewIds instanceof List) {
        List<?> previewIdList = (List<?>) previewIds;
        if (!previewIdList.isEmpty()) {
          Object firstId = previewIdList.get(0);
          if (firstId instanceof Map) {
            Map<String, Object> previewIdMap = (Map<String, Object>) firstId;
            placeRequest.put("PreviewId", previewIdMap.get("previewId"));
          }
        }
      } else if (previewIds instanceof Map) {
        Map<String, Object> previewIdMap = (Map<String, Object>) previewIds;
        placeRequest.put("PreviewId", previewIdMap.get("previewId"));
      }
    }

    // Copy Order structure from preview request
    if (previewReq != null && previewReq.containsKey("Order")) {
      placeRequest.put("Order", previewReq.get("Order"));
    }

    request.put("PlaceOrderRequest", placeRequest);

    return request;
  }
}
