package com.myqyl.aitradex.etrade.api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAlertsAPI;
import com.myqyl.aitradex.etrade.service.EtradeAlertsService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

/**
 * Integration tests for E*TRADE Alerts API endpoints.
 * 
 * These tests validate our Alerts REST API endpoints by:
 * - Calling our REST API endpoints (via MockMvc)
 * - Mocking the underlying Alerts API client
 * - Validating request/response handling, error handling, etc.
 */
@DisplayName("E*TRADE Alerts API Integration Tests")
class EtradeAlertsApiIntegrationTest extends EtradeApiIntegrationTestBase {

  @MockBean
  private EtradeAlertsService alertsService;

  @MockBean
  private EtradeApiClientAlertsAPI alertsApi;

  @BeforeEach
  void setUpAlerts() {
    // Additional setup for Alerts tests if needed
  }

  @Test
  @DisplayName("GET /api/etrade/alerts should return alerts list")
  void listAlerts_shouldReturnAlertsList() throws Exception {
    UUID accountId = testAccountId;

    AlertsResponse response = createTestAlertsResponse();

    when(alertsService.listAlerts(eq(accountId), any(ListAlertsRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString())
            .param("count", "25")
            .param("category", "STOCK")
            .param("status", "UNREAD")
            .param("direction", "DESC")
            .param("search", "AAPL"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalAlerts").value(148))
        .andExpect(jsonPath("$.alerts").isArray())
        .andExpect(jsonPath("$.alerts[0].id").value(6774))
        .andExpect(jsonPath("$.alerts[0].subject").value("Transfer failed-Insufficient Funds"))
        .andExpect(jsonPath("$.alerts[0].status").value("UNREAD"));

    verify(alertsService, times(1)).listAlerts(eq(accountId), any(ListAlertsRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/alerts/{alertId} should return alert details")
  void getAlertDetails_shouldReturnAlertDetails() throws Exception {
    UUID accountId = testAccountId;
    String alertId = "6773";

    AlertDetailsDto details = createTestAlertDetails();

    when(alertsService.getAlertDetails(eq(accountId), any(GetAlertDetailsRequest.class)))
        .thenReturn(details);

    mockMvc.perform(get("/api/etrade/alerts/{alertId}", alertId)
            .param("accountId", accountId.toString())
            .param("tags", "true"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(6773))
        .andExpect(jsonPath("$.subject").value("AAPL down by at least 2.00%"))
        .andExpect(jsonPath("$.symbol").value("AAPL"))
        .andExpect(jsonPath("$.msgText").exists())
        .andExpect(jsonPath("$.next").exists())
        .andExpect(jsonPath("$.prev").exists());

    verify(alertsService, times(1)).getAlertDetails(eq(accountId), any(GetAlertDetailsRequest.class));
  }

  @Test
  @DisplayName("DELETE /api/etrade/alerts/{alertIdList} should delete alerts")
  void deleteAlerts_shouldDeleteAlerts() throws Exception {
    UUID accountId = testAccountId;
    String alertIdList = "6772,6774";

    DeleteAlertsResponse response = createTestDeleteAlertsResponse();

    when(alertsService.deleteAlerts(eq(accountId), any(DeleteAlertsRequest.class)))
        .thenReturn(response);

    mockMvc.perform(delete("/api/etrade/alerts/{alertIdList}", alertIdList)
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.result").value("Success"))
        .andExpect(jsonPath("$.failedAlerts").isArray())
        .andExpect(jsonPath("$.failedAlerts").isEmpty());

    verify(alertsService, times(1)).deleteAlerts(eq(accountId), any(DeleteAlertsRequest.class));
  }

  @Test
  @DisplayName("DELETE /api/etrade/alerts/{alertIdList} should handle failed alerts")
  void deleteAlerts_shouldHandleFailedAlerts() throws Exception {
    UUID accountId = testAccountId;
    String alertIdList = "6772,6775";

    DeleteAlertsResponse response = createTestDeleteAlertsResponseWithFailures();

    when(alertsService.deleteAlerts(eq(accountId), any(DeleteAlertsRequest.class)))
        .thenReturn(response);

    mockMvc.perform(delete("/api/etrade/alerts/{alertIdList}", alertIdList)
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.result").value("Success"))
        .andExpect(jsonPath("$.failedAlerts").isArray())
        .andExpect(jsonPath("$.failedAlerts[0].id").value(6775))
        .andExpect(jsonPath("$.failedAlerts[0].reason").value("Alert not found"));

    verify(alertsService, times(1)).deleteAlerts(eq(accountId), any(DeleteAlertsRequest.class));
  }

  @Test
  @DisplayName("GET /api/etrade/alerts should handle empty alerts")
  void listAlerts_shouldHandleEmptyAlerts() throws Exception {
    UUID accountId = testAccountId;

    AlertsResponse response = new AlertsResponse();
    response.setTotalAlerts(0L);
    response.setAlerts(List.of());

    when(alertsService.listAlerts(eq(accountId), any(ListAlertsRequest.class)))
        .thenReturn(response);

    mockMvc.perform(get("/api/etrade/alerts")
            .param("accountId", accountId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalAlerts").value(0))
        .andExpect(jsonPath("$.alerts").isArray())
        .andExpect(jsonPath("$.alerts").isEmpty());
  }

  // Helper methods

  private AlertsResponse createTestAlertsResponse() {
    AlertsResponse response = new AlertsResponse();
    response.setTotalAlerts(148L);

    AlertDto alert1 = new AlertDto();
    alert1.setId(6774L);
    alert1.setCreateTime(1529426402L);
    alert1.setSubject("Transfer failed-Insufficient Funds");
    alert1.setStatus("UNREAD");

    AlertDto alert2 = new AlertDto();
    alert2.setId(6773L);
    alert2.setCreateTime(1529416825L);
    alert2.setSubject("AAPL down by at least 2.00%");
    alert2.setStatus("UNREAD");

    AlertDto alert3 = new AlertDto();
    alert3.setId(6772L);
    alert3.setCreateTime(1529393902L);
    alert3.setSubject("External Account Added");
    alert3.setStatus("UNREAD");

    response.setAlerts(Arrays.asList(alert1, alert2, alert3));

    return response;
  }

  private AlertDetailsDto createTestAlertDetails() {
    AlertDetailsDto details = new AlertDetailsDto();
    details.setId(6773L);
    details.setCreateTime(1529416825L);
    details.setSubject("AAPL down by at least 2.00%");
    details.setSymbol("AAPL");
    details.setMsgText("APPLE INC COM (AAPL) stock has met your target percentage decrease of 2.00%.");
    details.setReadTime(0L);
    details.setDeleteTime(0L);
    details.setNext("https://api.etrade.com/v1/user/alerts/6772");
    details.setPrev("https://api.etrade.com/v1/user/alerts/6774");

    return details;
  }

  private DeleteAlertsResponse createTestDeleteAlertsResponse() {
    DeleteAlertsResponse response = new DeleteAlertsResponse();
    response.setResult("Success");
    response.setFailedAlerts(List.of());
    return response;
  }

  private DeleteAlertsResponse createTestDeleteAlertsResponseWithFailures() {
    DeleteAlertsResponse response = new DeleteAlertsResponse();
    response.setResult("Success");

    FailedAlertDto failedAlert = new FailedAlertDto();
    failedAlert.setId(6775L);
    failedAlert.setReason("Alert not found");

    response.setFailedAlerts(List.of(failedAlert));

    return response;
  }
}
