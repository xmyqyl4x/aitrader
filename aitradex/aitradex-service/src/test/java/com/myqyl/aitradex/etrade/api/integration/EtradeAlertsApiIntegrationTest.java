package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.api.integration.EtradeApiIntegrationTestBase;
import com.myqyl.aitradex.etrade.client.EtradeAlertsClient;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE Alerts API endpoints.
 * 
 * These tests validate our application's alerts functionality by:
 * - Calling our REST API endpoints (/api/etrade/alerts/*)
 * - Mocking the underlying E*TRADE client calls
 * - Validating request building, response parsing, error handling
 * 
 * Tests do NOT call E*TRADE's public endpoints directly.
 */
@DisplayName("E*TRADE Alerts API Integration Tests")
class EtradeAlertsApiIntegrationTest extends EtradeApiIntegrationTestBase {

  // ============================================================================
  // 1. LIST ALERTS TESTS
  // ============================================================================

  @Test
  @DisplayName("List Alerts - Success")
  void listAlerts_success() throws Exception {
    // Mock E*TRADE client response
    List<Map<String, Object>> mockAlerts = List.of(
        createMockAlert("ALERT123", "Price Alert", "ACTIVE", "PRICE"),
        createMockAlert("ALERT456", "News Alert", "READ", "NEWS")
    );
    when(alertsClient.listAlerts(eq(testAccountId), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(mockAlerts);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value("ALERT123"))
        .andExpect(jsonPath("$[0].subject").value("Price Alert"))
        .andExpect(jsonPath("$[1].id").value("ALERT456"));

    verify(alertsClient, times(1)).listAlerts(eq(testAccountId), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  @DisplayName("List Alerts - With Filters")
  void listAlerts_withFilters() throws Exception {
    List<Map<String, Object>> mockAlerts = List.of(
        createMockAlert("ALERT789", "Price Alert", "ACTIVE", "PRICE")
    );
    when(alertsClient.listAlerts(eq(testAccountId), eq(10), eq("PRICE"), eq("ACTIVE"), eq("DESC"), isNull()))
        .thenReturn(mockAlerts);

    mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", testAccountId.toString())
            .param("count", "10")
            .param("category", "PRICE")
            .param("status", "ACTIVE")
            .param("direction", "DESC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].category").value("PRICE"));

    verify(alertsClient, times(1)).listAlerts(eq(testAccountId), eq(10), eq("PRICE"), eq("ACTIVE"), eq("DESC"), isNull());
  }

  @Test
  @DisplayName("List Alerts - With Search")
  void listAlerts_withSearch() throws Exception {
    List<Map<String, Object>> mockAlerts = List.of(
        createMockAlert("ALERT999", "AAPL Price Alert", "ACTIVE", "PRICE")
    );
    when(alertsClient.listAlerts(eq(testAccountId), isNull(), isNull(), isNull(), isNull(), eq("AAPL")))
        .thenReturn(mockAlerts);

    mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", testAccountId.toString())
            .param("search", "AAPL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].subject").value("AAPL Price Alert"));

    verify(alertsClient, times(1)).listAlerts(eq(testAccountId), isNull(), isNull(), isNull(), isNull(), eq("AAPL"));
  }

  // ============================================================================
  // 2. GET ALERT DETAILS TESTS
  // ============================================================================

  @Test
  @DisplayName("Get Alert Details - Success")
  void getAlertDetails_success() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockDetails = createMockAlertDetails("ALERT123", "Price Alert", "ACTIVE", "PRICE", "AAPL reached $150");
    when(alertsClient.getAlertDetails(eq(testAccountId), eq("ALERT123"), isNull()))
        .thenReturn(mockDetails);

    // Call our application endpoint
    mockMvc.perform(get("/api/etrade/alerts/{alertId}", "ALERT123")
            .param("accountId", testAccountId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("ALERT123"))
        .andExpect(jsonPath("$.subject").value("Price Alert"))
        .andExpect(jsonPath("$.message").value("AAPL reached $150"));

    verify(alertsClient, times(1)).getAlertDetails(eq(testAccountId), eq("ALERT123"), isNull());
  }

  @Test
  @DisplayName("Get Alert Details - With HTML Tags")
  void getAlertDetails_withHtmlTags() throws Exception {
    Map<String, Object> mockDetails = createMockAlertDetails("ALERT123", "Price Alert", "ACTIVE", "PRICE", "<p>AAPL reached $150</p>");
    when(alertsClient.getAlertDetails(eq(testAccountId), eq("ALERT123"), eq(true)))
        .thenReturn(mockDetails);

    mockMvc.perform(get("/api/etrade/alerts/{alertId}", "ALERT123")
            .param("accountId", testAccountId.toString())
            .param("htmlTags", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("<p>AAPL reached $150</p>"));

    verify(alertsClient, times(1)).getAlertDetails(eq(testAccountId), eq("ALERT123"), eq(true));
  }

  // ============================================================================
  // 3. DELETE ALERTS TESTS
  // ============================================================================

  @Test
  @DisplayName("Delete Alerts - Success")
  void deleteAlerts_success() throws Exception {
    // Mock E*TRADE client response
    Map<String, Object> mockResult = Map.of("success", true, "message", "Alerts deleted successfully");
    when(alertsClient.deleteAlerts(eq(testAccountId), eq(List.of("ALERT123", "ALERT456"))))
        .thenReturn(mockResult);

    // Call our application endpoint
    mockMvc.perform(delete("/api/etrade/alerts")
            .param("accountId", testAccountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of("ALERT123", "ALERT456"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Alerts deleted successfully"));

    verify(alertsClient, times(1)).deleteAlerts(eq(testAccountId), eq(List.of("ALERT123", "ALERT456")));
  }

  @Test
  @DisplayName("Delete Alerts - Single Alert")
  void deleteAlerts_singleAlert() throws Exception {
    Map<String, Object> mockResult = Map.of("success", true, "message", "Alert deleted");
    when(alertsClient.deleteAlerts(eq(testAccountId), eq(List.of("ALERT123"))))
        .thenReturn(mockResult);

    mockMvc.perform(delete("/api/etrade/alerts")
            .param("accountId", testAccountId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of("ALERT123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(alertsClient, times(1)).deleteAlerts(eq(testAccountId), eq(List.of("ALERT123")));
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private Map<String, Object> createMockAlert(String id, String subject, String status, String category) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("id", id);
    alert.put("subject", subject);
    alert.put("status", status);
    alert.put("category", category);
    alert.put("dateTime", "2024-01-15T10:00:00-05:00");
    alert.put("priority", "HIGH");
    return alert;
  }

  private Map<String, Object> createMockAlertDetails(String id, String subject, String status, 
                                                      String category, String message) {
    Map<String, Object> details = new HashMap<>();
    details.put("id", id);
    details.put("subject", subject);
    details.put("status", status);
    details.put("category", category);
    details.put("message", message);
    details.put("dateTime", "2024-01-15T10:00:00-05:00");
    details.put("priority", "HIGH");
    return details;
  }
}
