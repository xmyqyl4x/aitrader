package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import com.myqyl.aitradex.etrade.orders.dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeApiClientOrderAPI.
 * 
 * Tests all 6 Order API endpoints:
 * 1. List Orders
 * 2. Preview Order
 * 3. Place Order
 * 4. Cancel Order
 * 5. Change Previewed Order
 * 6. Place Changed Order
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeApiClientOrderAPI Tests")
class EtradeApiClientOrderAPITest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeApiClientOrderAPI orderApi;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
  private static final String TEST_ACCOUNT_ID_KEY = "12345678";
  private static final String TEST_ORDER_ID = "987654321";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    orderApi = new EtradeApiClientOrderAPI(apiClient, properties, objectMapper);

    when(properties.getOrdersUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders");
    when(properties.getOrderPreviewUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/preview");
    when(properties.getOrderPlaceUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/place");
    when(properties.getOrderCancelUrl(TEST_ACCOUNT_ID_KEY))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/cancel");
    when(properties.getOrderChangePreviewUrl(TEST_ACCOUNT_ID_KEY, TEST_ORDER_ID))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/" + TEST_ORDER_ID + "/change/preview");
    when(properties.getOrderChangePlaceUrl(TEST_ACCOUNT_ID_KEY, TEST_ORDER_ID))
        .thenReturn("https://apisb.etrade.com/v1/accounts/" + TEST_ACCOUNT_ID_KEY + "/orders/" + TEST_ORDER_ID + "/change/place");
  }

  @Test
  @DisplayName("listOrders - Success")
  void listOrders_shouldReturnOrdersList() throws Exception {
    String responseJson = "{\"OrdersResponse\":{" +
        "\"Order\":{\"orderId\":\"" + TEST_ORDER_ID + "\",\"orderType\":\"EQ\"," +
        "\"orderStatus\":\"OPEN\",\"accountId\":\"840104290\",\"clientOrderId\":\"CLIENT123\"," +
        "\"placedTime\":1234567890000,\"OrderDetail\":{\"priceType\":\"MARKET\"," +
        "\"orderTerm\":\"GOOD_FOR_DAY\",\"marketSession\":\"REGULAR\"," +
        "\"Instrument\":{\"orderAction\":\"BUY\",\"quantity\":100," +
        "\"quantityType\":\"QUANTITY\",\"Product\":{\"symbol\":\"AAPL\"," +
        "\"securityType\":\"EQ\"}}}}}," +
        "\"marker\":\"MARKER123\",\"moreOrders\":false}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    ListOrdersRequest request = new ListOrdersRequest();
    request.setCount(25);
    request.setStatus("OPEN");

    OrdersResponse result = orderApi.listOrders(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertNotNull(result.getOrders());
    assertEquals(1, result.getOrders().size());
    EtradeOrderModel order = result.getOrders().get(0);
    assertEquals(TEST_ORDER_ID, order.getOrderId());
    assertEquals("EQ", order.getOrderType());
    assertEquals("OPEN", order.getOrderStatus());
    assertEquals("MARKER123", result.getMarker());
    assertFalse(result.getMoreOrders());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("previewOrder - Success")
  void previewOrder_shouldReturnPreviewResponse() throws Exception {
    String responseJson = "{\"PreviewOrderResponse\":{" +
        "\"accountId\":\"840104290\"," +
        "\"PreviewIds\":[{\"previewId\":\"PREVIEW123\"}]," +
        "\"Order\":{\"orderId\":null,\"orderType\":\"EQ\"," +
        "\"OrderDetail\":{\"priceType\":\"MARKET\",\"estimatedCommission\":1.99," +
        "\"estimatedTotalAmount\":10001.99}}," +
        "\"totalOrderValue\":10000.00,\"estimatedCommission\":1.99," +
        "\"estimatedTotalAmount\":10001.99}}";

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PreviewOrderRequest request = createTestPreviewOrderRequest();

    PreviewOrderResponse result = orderApi.previewOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertEquals("840104290", result.getAccountId());
    assertNotNull(result.getPreviewIds());
    assertEquals(1, result.getPreviewIds().size());
    assertEquals("PREVIEW123", result.getPreviewIds().get(0).getPreviewId());
    assertEquals(10000.00, result.getTotalOrderValue());
    assertEquals(1.99, result.getEstimatedCommission());
    assertEquals(10001.99, result.getEstimatedTotalAmount());

    verify(apiClient, times(1)).makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("placeOrder - Success")
  void placeOrder_shouldReturnPlaceResponse() throws Exception {
    String responseJson = "{\"PlaceOrderResponse\":{" +
        "\"OrderIds\":[{\"orderId\":\"" + TEST_ORDER_ID + "\"}]," +
        "\"Messages\":[]}}";

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PlaceOrderRequest request = createTestPlaceOrderRequest();

    PlaceOrderResponse result = orderApi.placeOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertNotNull(result.getOrderIds());
    assertEquals(1, result.getOrderIds().size());
    assertEquals(TEST_ORDER_ID, result.getOrderIds().get(0).getOrderId());

    verify(apiClient, times(1)).makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("cancelOrder - Success")
  void cancelOrder_shouldReturnCancelResponse() throws Exception {
    String responseJson = "{\"Messages\":[]}";

    when(apiClient.makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    CancelOrderRequest request = new CancelOrderRequest(TEST_ORDER_ID);

    CancelOrderResponse result = orderApi.cancelOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertTrue(result.getSuccess());
    assertNotNull(result.getMessages());

    verify(apiClient, times(1)).makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("changePreviewOrder - Success")
  void changePreviewOrder_shouldReturnPreviewResponse() throws Exception {
    String responseJson = "{\"PreviewOrderResponse\":{" +
        "\"accountId\":\"840104290\"," +
        "\"PreviewIds\":[{\"previewId\":\"PREVIEW456\"}]}}";

    when(apiClient.makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PreviewOrderRequest request = createTestPreviewOrderRequest();

    PreviewOrderResponse result = orderApi.changePreviewOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, TEST_ORDER_ID, request);

    assertNotNull(result);
    assertEquals("840104290", result.getAccountId());
    assertNotNull(result.getPreviewIds());
    assertEquals(1, result.getPreviewIds().size());
    assertEquals("PREVIEW456", result.getPreviewIds().get(0).getPreviewId());

    verify(apiClient, times(1)).makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("placeChangedOrder - Success")
  void placeChangedOrder_shouldReturnPlaceResponse() throws Exception {
    String responseJson = "{\"PlaceOrderResponse\":{" +
        "\"OrderIds\":[{\"orderId\":\"" + TEST_ORDER_ID + "\"}]," +
        "\"Messages\":[]}}";

    when(apiClient.makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    PlaceOrderRequest request = createTestPlaceOrderRequest();

    PlaceOrderResponse result = orderApi.placeChangedOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, TEST_ORDER_ID, request);

    assertNotNull(result);
    assertNotNull(result.getOrderIds());
    assertEquals(1, result.getOrderIds().size());
    assertEquals(TEST_ORDER_ID, result.getOrderIds().get(0).getOrderId());

    verify(apiClient, times(1)).makeRequest(eq("PUT"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("previewOrder - Error handling")
  void previewOrder_shouldHandleErrorResponse() throws Exception {
    String errorJson = "{\"Messages\":[{" +
        "\"type\":\"ERROR\"," +
        "\"code\":\"100\"," +
        "\"description\":\"Invalid order request\"}]}";

    when(apiClient.makeRequest(eq("POST"), anyString(), isNull(), anyString(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorJson);

    PreviewOrderRequest request = createTestPreviewOrderRequest();

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        orderApi.previewOrder(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request));

    assertEquals(400, exception.getStatusCode());
    assertEquals("PREVIEW_ORDER_FAILED", exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Invalid order request"));
  }

  @Test
  @DisplayName("listOrders - Handles empty orders")
  void listOrders_shouldHandleEmptyOrders() throws Exception {
    String responseJson = "{\"OrdersResponse\":{" +
        "\"Order\":[]," +
        "\"marker\":null,\"moreOrders\":false}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    ListOrdersRequest request = new ListOrdersRequest();

    OrdersResponse result = orderApi.listOrders(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_KEY, request);

    assertNotNull(result);
    assertNotNull(result.getOrders());
    assertEquals(0, result.getOrders().size());
    assertFalse(result.getMoreOrders());
  }

  // Helper methods

  private PreviewOrderRequest createTestPreviewOrderRequest() {
    PreviewOrderRequest request = new PreviewOrderRequest();
    request.setOrderType("EQ");
    request.setClientOrderId("CLIENT123");

    OrderDetailDto orderDetail = new OrderDetailDto();
    orderDetail.setPriceType("MARKET");
    orderDetail.setOrderTerm("GOOD_FOR_DAY");
    orderDetail.setMarketSession("REGULAR");
    orderDetail.setAllOrNone(false);

    OrderInstrumentDto instrument = new OrderInstrumentDto();
    instrument.setOrderAction("BUY");
    instrument.setQuantity(100);
    instrument.setQuantityType("QUANTITY");

    OrderProductDto product = new OrderProductDto();
    product.setSymbol("AAPL");
    product.setSecurityType("EQ");
    instrument.setProduct(product);

    orderDetail.setInstruments(List.of(instrument));
    request.setOrders(List.of(orderDetail));

    return request;
  }

  private PlaceOrderRequest createTestPlaceOrderRequest() {
    PlaceOrderRequest request = new PlaceOrderRequest();
    request.setPreviewId("PREVIEW123");
    request.setOrderType("EQ");
    request.setClientOrderId("CLIENT123");

    OrderDetailDto orderDetail = new OrderDetailDto();
    orderDetail.setPriceType("MARKET");
    orderDetail.setOrderTerm("GOOD_FOR_DAY");
    orderDetail.setMarketSession("REGULAR");

    OrderInstrumentDto instrument = new OrderInstrumentDto();
    instrument.setOrderAction("BUY");
    instrument.setQuantity(100);
    instrument.setQuantityType("QUANTITY");

    OrderProductDto product = new OrderProductDto();
    product.setSymbol("AAPL");
    product.setSecurityType("EQ");
    instrument.setProduct(product);

    orderDetail.setInstruments(List.of(instrument));
    request.setOrders(List.of(orderDetail));

    return request;
  }
}
