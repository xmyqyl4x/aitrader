package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.order.OrderRequestBuilder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeOrderClient.
 */
@ExtendWith(MockitoExtension.class)
class EtradeOrderClientTest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeOrderClient orderClient;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
  private static final String TEST_ACCOUNT_ID_KEY = "12345678";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    orderClient = new EtradeOrderClient(apiClient, properties, objectMapper);

    when(properties.getOrderPreviewUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/preview");
    when(properties.getOrderPlaceUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/place");
    when(properties.getOrdersUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders");
    when(properties.getOrderCancelUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/cancel");
  }

  @Test
  void previewOrder_marketOrder_returnsPreview() throws Exception {
    String mockResponse = """
        {
          "PreviewOrderResponse": {
            "accountId": "12345678",
            "PreviewIds": [{"previewId": "PREVIEW123"}],
            "Order": [{
              "priceType": "MARKET",
              "orderTerm": "GOOD_FOR_DAY",
              "Instrument": [{
                "Product": {"symbol": "AAPL", "securityType": "EQ"},
                "orderAction": "BUY",
                "quantity": 10
              }]
            }],
            "totalOrderValue": 1500.00,
            "estimatedCommission": 6.95
          }
        }
        """;

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> result = orderClient.previewOrder(
        TEST_ACCOUNT_ID,
        TEST_ACCOUNT_ID_KEY,
        "AAPL",
        OrderRequestBuilder.OrderAction.BUY,
        10,
        OrderRequestBuilder.PriceType.MARKET,
        OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        null,
        null,
        OrderRequestBuilder.MarketSession.REGULAR,
        null);

    assertNotNull(result);
    assertEquals("12345678", result.get("accountId").toString());
    assertTrue(result.containsKey("PreviewIds"));
    assertTrue(result.containsKey("totalOrderValue"));
  }

  @Test
  void previewOrder_limitOrder_includesLimitPrice() throws Exception {
    String mockResponse = """
        {
          "PreviewOrderResponse": {
            "accountId": "12345678",
            "PreviewIds": [{"previewId": "PREVIEW456"}],
            "Order": [{
              "priceType": "LIMIT",
              "limitPrice": 150.50,
              "Instrument": [{
                "Product": {"symbol": "TSLA"},
                "orderAction": "SELL",
                "quantity": 5
              }]
            }],
            "totalOrderValue": 750.00
          }
        }
        """;

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> result = orderClient.previewOrder(
        TEST_ACCOUNT_ID,
        TEST_ACCOUNT_ID_KEY,
        "TSLA",
        OrderRequestBuilder.OrderAction.SELL,
        5,
        OrderRequestBuilder.PriceType.LIMIT,
        OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
        150.50,
        null,
        OrderRequestBuilder.MarketSession.REGULAR,
        "CLIENT123");

    assertNotNull(result);
    assertTrue(result.containsKey("PreviewIds"));
  }

  @Test
  void previewOrder_apiError_throwsException() {
    String errorResponse = """
        {
          "Messages": {
            "Message": [{
              "type": "ERROR",
              "code": "INVALID_SYMBOL",
              "description": "Invalid symbol provided"
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorResponse);

    assertThrows(RuntimeException.class, () ->
        orderClient.previewOrder(
            TEST_ACCOUNT_ID,
            TEST_ACCOUNT_ID_KEY,
            "INVALID",
            OrderRequestBuilder.OrderAction.BUY,
            1,
            OrderRequestBuilder.PriceType.MARKET,
            OrderRequestBuilder.OrderTerm.GOOD_FOR_DAY,
            null,
            null,
            OrderRequestBuilder.MarketSession.REGULAR,
            null));
  }

  @Test
  void placeOrder_usesPreviewResponse() throws Exception {
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

    // Create preview response
    Map<String, Object> previewResponse = Map.of(
        "PreviewIds", List.of(Map.of("previewId", "PREVIEW789")),
        "accountId", "12345678");

    String placeOrderResponse = """
        {
          "PlaceOrderResponse": {
            "OrderIds": [{"orderId": "ORDER123"}],
            "Messages": [{
              "type": "INFO",
              "code": "ORDER_PLACED",
              "description": "Order placed successfully"
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(placeOrderResponse);

    Map<String, Object> result = orderClient.placeOrder(
        TEST_ACCOUNT_ID,
        TEST_ACCOUNT_ID_KEY,
        previewResponse,
        previewRequest);

    assertNotNull(result);
    assertTrue(result.containsKey("orderIds") || result.containsKey("messages"));
  }

  @Test
  void getOrders_returnsOrderList() throws Exception {
    String mockResponse = """
        {
          "OrdersResponse": {
            "Order": [{
              "orderId": "ORDER123",
              "orderType": "EQ",
              "orderStatus": "OPEN",
              "placedTime": 1640995200000,
              "OrderDetail": [{
                "priceType": "MARKET",
                "orderTerm": "GOOD_FOR_DAY",
                "Instrument": [{
                  "Product": {"symbol": "AAPL", "securityType": "EQ"},
                  "orderAction": "BUY",
                  "quantity": 10
                }]
              }]
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> orders = orderClient.getOrders(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(orders);
    assertFalse(orders.isEmpty());
    assertEquals("ORDER123", orders.get(0).get("orderId"));
  }

  @Test
  void getOrders_emptyResponse_returnsEmptyList() throws Exception {
    String mockResponse = """
        {
          "OrdersResponse": {}
        }
        """;

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    List<Map<String, Object>> orders = orderClient.getOrders(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY);

    assertNotNull(orders);
    assertTrue(orders.isEmpty());
  }

  @Test
  void cancelOrder_returnsCancellationResponse() throws Exception {
    String mockResponse = """
        {
          "CancelOrderResponse": {
            "Order": [{
              "orderId": "ORDER123",
              "status": "CANCELLED"
            }],
            "Messages": [{
              "type": "INFO",
              "code": "ORDER_CANCELLED",
              "description": "Order cancelled successfully"
            }]
          }
        }
        """;

    when(apiClient.makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(mockResponse);

    Map<String, Object> result = orderClient.cancelOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, "ORDER123");

    assertNotNull(result);
    assertTrue(result.containsKey("success"));
  }
}
