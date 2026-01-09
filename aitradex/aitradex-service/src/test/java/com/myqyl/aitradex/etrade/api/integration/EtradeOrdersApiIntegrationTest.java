package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.EtradeOrderDto;
import com.myqyl.aitradex.etrade.client.EtradeApiClientOrderAPI;
import com.myqyl.aitradex.etrade.orders.dto.*;
import com.myqyl.aitradex.etrade.service.EtradeOrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE Orders API endpoints.
 * 
 * These tests validate our Orders REST API endpoints by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying Order API client
 * - Validating request/response handling, error handling, etc.
 */
@DisplayName("E*TRADE Orders API Integration Tests")
class EtradeOrdersApiIntegrationTest extends EtradeApiIntegrationTestBase {

  @MockBean
  private EtradeOrderService orderService;

  @MockBean
  private EtradeApiClientOrderAPI orderApi;

  private UUID testOrderId;

  @BeforeEach
  void setUpOrders() {
    testOrderId = UUID.randomUUID();
  }

  @Test
  @DisplayName("POST /api/etrade/orders/preview should return preview response")
  void previewOrder_shouldReturnPreviewResponse() throws Exception {
    UUID accountId = testAccountId;

    PreviewOrderRequest request = createTestPreviewOrderRequest();
    PreviewOrderResponse response = createTestPreviewOrderResponse();

    when(orderService.previewOrder(eq(accountId), any(PreviewOrderRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/etrade/orders/preview")
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accountId").value("840104290"))
        .andExpect(jsonPath("$.previewIds").isArray())
        .andExpect(jsonPath("$.previewIds[0].previewId").value("PREVIEW123"))
        .andExpect(jsonPath("$.totalOrderValue").value(10000.0))
        .andExpect(jsonPath("$.estimatedCommission").value(1.99));

    verify(orderService, times(1)).previewOrder(eq(accountId), any(PreviewOrderRequest.class));
  }

  @Test
  @DisplayName("POST /api/etrade/orders should place order and return order DTO")
  void placeOrder_shouldReturnOrderDto() throws Exception {
    UUID accountId = testAccountId;

    PlaceOrderRequest request = createTestPlaceOrderRequest();
    EtradeOrderDto orderDto = createTestEtradeOrderDto();

    when(orderService.placeOrder(eq(accountId), any(PlaceOrderRequest.class)))
        .thenReturn(orderDto);

    mockMvc.perform(post("/api/etrade/orders")
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(testOrderId.toString()))
        .andExpect(jsonPath("$.accountId").value(accountId.toString()))
        .andExpect(jsonPath("$.symbol").value("AAPL"))
        .andExpect(jsonPath("$.quantity").value(100))
        .andExpect(jsonPath("$.side").value("BUY"));

    verify(orderService, times(1)).placeOrder(eq(accountId), any(PlaceOrderRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/orders/list should return orders list")
  void listOrders_shouldReturnOrdersList() throws Exception {
    UUID accountId = testAccountId;

    OrdersResponse response = createTestOrdersResponse();

    when(orderService.listOrders(eq(accountId), any(ListOrdersRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/orders/list")
            .param("accountId", accountId.toString())
            .param("count", "25")
            .param("status", "OPEN"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.orders").isArray())
        .andExpect(jsonPath("$.orders[0].orderId").value("987654321"))
        .andExpect(jsonPath("$.orders[0].orderType").value("EQ"))
        .andExpect(jsonPath("$.orders[0].orderStatus").value("OPEN"))
        .andExpect(jsonPath("$.moreOrders").value(false));

    verify(orderService, times(1)).listOrders(eq(accountId), any(ListOrdersRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/orders should return paged orders from database")
  void getOrders_shouldReturnPagedOrders() throws Exception {
    UUID accountId = testAccountId;

    Pageable pageable = PageRequest.of(0, 20);
    Page<EtradeOrderDto> orderPage = new PageImpl<>(List.of(createTestEtradeOrderDto()), pageable, 1);

    when(orderService.getOrders(eq(accountId), any(Pageable.class)))
        .thenReturn(orderPage);

    mockMvc.perform(get("/api/etrade/orders")
            .param("accountId", accountId.toString())
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(testOrderId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    verify(orderService, times(1)).getOrders(eq(accountId), any(Pageable.class));
  }

  @Test
  @DisplayName("PUT /api/etrade/orders/{orderId}/preview should return preview response")
  void changePreviewOrder_shouldReturnPreviewResponse() throws Exception {
    UUID accountId = testAccountId;
    UUID orderId = testOrderId;

    PreviewOrderRequest request = createTestPreviewOrderRequest();
    PreviewOrderResponse response = createTestPreviewOrderResponse();

    when(orderService.changePreviewOrder(eq(accountId), eq(orderId), any(PreviewOrderRequest.class)))
        .thenReturn(response);

    mockMvc.perform(put("/api/etrade/orders/{orderId}/preview", orderId)
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accountId").value("840104290"))
        .andExpect(jsonPath("$.previewIds[0].previewId").value("PREVIEW123"));

    verify(orderService, times(1)).changePreviewOrder(eq(accountId), eq(orderId), any(PreviewOrderRequest.class));
  }

  @Test
  @DisplayName("PUT /api/etrade/orders/{orderId} should place changed order and return order DTO")
  void placeChangedOrder_shouldReturnOrderDto() throws Exception {
    UUID accountId = testAccountId;
    UUID orderId = testOrderId;

    PlaceOrderRequest request = createTestPlaceOrderRequest();
    EtradeOrderDto orderDto = createTestEtradeOrderDto();

    when(orderService.placeChangedOrder(eq(accountId), eq(orderId), any(PlaceOrderRequest.class)))
        .thenReturn(orderDto);

    mockMvc.perform(put("/api/etrade/orders/{orderId}", orderId)
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(testOrderId.toString()))
        .andExpect(jsonPath("$.symbol").value("AAPL"));

    verify(orderService, times(1)).placeChangedOrder(eq(accountId), eq(orderId), any(PlaceOrderRequest.class));
  }

  @Test
  @DisplayName("DELETE /api/etrade/orders/{orderId} should cancel order and return cancel response")
  void cancelOrder_shouldReturnCancelResponse() throws Exception {
    UUID accountId = testAccountId;
    UUID orderId = testOrderId;

    CancelOrderResponse response = new CancelOrderResponse();
    response.setSuccess(true);

    when(orderService.cancelOrder(eq(accountId), eq(orderId)))
        .thenReturn(response);

    mockMvc.perform(delete("/api/etrade/orders/{orderId}", orderId)
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true));

    verify(orderService, times(1)).cancelOrder(eq(accountId), eq(orderId));
  }

  @Test
  @DisplayName("POST /api/etrade/orders/preview should handle validation errors")
  void previewOrder_shouldHandleValidationErrors() throws Exception {
    UUID accountId = testAccountId;

    // Invalid request - missing orderType
    PreviewOrderRequest request = new PreviewOrderRequest();
    request.setClientOrderId("CLIENT123");
    // orderType is null - should fail validation

    mockMvc.perform(post("/api/etrade/orders/preview")
            .param("accountId", accountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).previewOrder(any(), any());
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

  private PreviewOrderResponse createTestPreviewOrderResponse() {
    PreviewOrderResponse response = new PreviewOrderResponse();
    response.setAccountId("840104290");

    PreviewIdDto previewId = new PreviewIdDto("PREVIEW123");
    response.setPreviewIds(List.of(previewId));

    response.setTotalOrderValue(10000.0);
    response.setEstimatedCommission(1.99);
    response.setEstimatedTotalAmount(10001.99);

    return response;
  }

  private OrdersResponse createTestOrdersResponse() {
    OrdersResponse response = new OrdersResponse();

    EtradeOrderModel order = new EtradeOrderModel();
    order.setOrderId("987654321");
    order.setOrderType("EQ");
    order.setOrderStatus("OPEN");
    order.setAccountId("840104290");
    order.setClientOrderId("CLIENT123");

    OrderDetailDto orderDetail = new OrderDetailDto();
    orderDetail.setPriceType("MARKET");
    orderDetail.setOrderTerm("GOOD_FOR_DAY");

    OrderInstrumentDto instrument = new OrderInstrumentDto();
    instrument.setOrderAction("BUY");
    instrument.setQuantity(100);

    OrderProductDto product = new OrderProductDto();
    product.setSymbol("AAPL");
    product.setSecurityType("EQ");
    instrument.setProduct(product);

    orderDetail.setInstruments(List.of(instrument));
    order.setOrderDetails(List.of(orderDetail));

    response.setOrders(List.of(order));
    response.setMoreOrders(false);

    return response;
  }

  private EtradeOrderDto createTestEtradeOrderDto() {
    return new EtradeOrderDto(
        testOrderId,
        testAccountId,
        "987654321",
        "AAPL",
        "EQ",
        "MARKET",
        "BUY",
        100,
        BigDecimal.valueOf(150.00),
        null,
        "SUBMITTED",
        OffsetDateTime.now(),
        null,
        null,
        null,
        null);
  }
}
