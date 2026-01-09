package com.myqyl.aitradex.etrade.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EtradeApiClientAlertsAPI.
 * 
 * Tests all 3 Alerts API endpoints:
 * 1. List Alerts
 * 2. Get Alert Details
 * 3. Delete Alerts
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EtradeApiClientAlertsAPI Tests")
class EtradeApiClientAlertsAPITest {

  @Mock
  private EtradeApiClient apiClient;

  @Mock
  private EtradeProperties properties;

  private EtradeApiClientAlertsAPI alertsApi;
  private ObjectMapper objectMapper;
  private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    alertsApi = new EtradeApiClientAlertsAPI(apiClient, properties, objectMapper);

    when(properties.getAlertsListUrl())
        .thenReturn("https://apisb.etrade.com/v1/user/alerts");
    when(properties.getAlertDetailsUrl(anyString()))
        .thenAnswer(invocation -> "https://apisb.etrade.com/v1/user/alerts/" + invocation.getArgument(0));
    when(properties.getAlertsDeleteUrl(anyString()))
        .thenAnswer(invocation -> "https://apisb.etrade.com/v1/user/alerts/" + invocation.getArgument(0));
  }

  @Test
  @DisplayName("listAlerts - Success with all parameters")
  void listAlerts_shouldReturnAlertsList() throws Exception {
    String responseJson = "{\"AlertsResponse\":{" +
        "\"totalAlerts\":148," +
        "\"Alert\":[" +
        "{\"id\":6774,\"createTime\":1529426402,\"subject\":\"Transfer failed-Insufficient Funds\",\"status\":\"UNREAD\"}," +
        "{\"id\":6773,\"createTime\":1529416825,\"subject\":\"AAPL down by at least 2.00%\",\"status\":\"UNREAD\"}," +
        "{\"id\":6772,\"createTime\":1529393902,\"subject\":\"External Account Added\",\"status\":\"UNREAD\"}]}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    ListAlertsRequest request = new ListAlertsRequest();
    request.setCount(25);
    request.setCategory("STOCK");
    request.setStatus("UNREAD");
    request.setDirection("DESC");
    request.setSearch("AAPL");

    AlertsResponse result = alertsApi.listAlerts(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertEquals(148L, result.getTotalAlerts());
    assertNotNull(result.getAlerts());
    assertEquals(3, result.getAlerts().size());
    assertEquals(6774L, result.getAlerts().get(0).getId());
    assertEquals("Transfer failed-Insufficient Funds", result.getAlerts().get(0).getSubject());
    assertEquals("UNREAD", result.getAlerts().get(0).getStatus());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("listAlerts - Success with minimal parameters")
  void listAlerts_shouldReturnAlertsList_minimalParams() throws Exception {
    String responseJson = "{\"AlertsResponse\":{" +
        "\"totalAlerts\":5," +
        "\"Alert\":{\"id\":6774,\"createTime\":1529426402,\"subject\":\"Test Alert\",\"status\":\"READ\"}}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    ListAlertsRequest request = new ListAlertsRequest();

    AlertsResponse result = alertsApi.listAlerts(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertEquals(5L, result.getTotalAlerts());
    assertEquals(1, result.getAlerts().size());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("getAlertDetails - Success")
  void getAlertDetails_shouldReturnAlertDetails() throws Exception {
    String responseJson = "{\"AlertDetailsResponse\":{" +
        "\"id\":6773," +
        "\"createTime\":1529416825," +
        "\"subject\":\"AAPL down by at least 2.00%\"," +
        "\"symbol\":\"AAPL\"," +
        "\"msgText\":\"APPLE INC COM (AAPL) stock has met your target percentage decrease of 2.00%.\"," +
        "\"readTime\":0," +
        "\"deleteTime\":0," +
        "\"next\":\"https://api.etrade.com/v1/user/alerts/6772\"," +
        "\"prev\":\"https://api.etrade.com/v1/user/alerts/6774\"}}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    GetAlertDetailsRequest request = new GetAlertDetailsRequest();
    request.setId("6773");
    request.setTags(true);

    AlertDetailsDto result = alertsApi.getAlertDetails(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertEquals(6773L, result.getId());
    assertEquals("AAPL down by at least 2.00%", result.getSubject());
    assertEquals("AAPL", result.getSymbol());
    assertNotNull(result.getMsgText());
    assertEquals(0L, result.getReadTime());
    assertEquals(0L, result.getDeleteTime());
    assertNotNull(result.getNext());
    assertNotNull(result.getPrev());

    verify(apiClient, times(1)).makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("deleteAlerts - Success")
  void deleteAlerts_shouldReturnSuccess() throws Exception {
    String responseJson = "{\"AlertsResponse\":{\"result\":\"Success\"}}";

    when(apiClient.makeRequest(eq("DELETE"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    DeleteAlertsRequest request = new DeleteAlertsRequest();
    request.setAlertIds(Arrays.asList("6772", "6774"));

    DeleteAlertsResponse result = alertsApi.deleteAlerts(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Success", result.getResult());
    assertNotNull(result.getFailedAlerts());
    assertEquals(0, result.getFailedAlerts().size());

    verify(apiClient, times(1)).makeRequest(eq("DELETE"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("deleteAlerts - Success with failed alerts")
  void deleteAlerts_shouldReturnFailedAlerts() throws Exception {
    String responseJson = "{\"AlertsResponse\":{" +
        "\"result\":\"Success\"," +
        "\"FailedAlerts\":[" +
        "{\"id\":6775,\"reason\":\"Alert not found\"}]}}}";

    when(apiClient.makeRequest(eq("DELETE"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    DeleteAlertsRequest request = new DeleteAlertsRequest();
    request.setAlertIds(Arrays.asList("6772", "6775"));

    DeleteAlertsResponse result = alertsApi.deleteAlerts(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertNotNull(result.getFailedAlerts());
    assertEquals(1, result.getFailedAlerts().size());
    assertEquals(6775L, result.getFailedAlerts().get(0).getId());
    assertEquals("Alert not found", result.getFailedAlerts().get(0).getReason());

    verify(apiClient, times(1)).makeRequest(eq("DELETE"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID));
  }

  @Test
  @DisplayName("listAlerts - Error handling")
  void listAlerts_shouldHandleErrorResponse() throws Exception {
    String errorJson = "{\"Error\":{\"code\":\"53\",\"message\":\"There are currently no alerts in your inbox\"}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorJson);

    ListAlertsRequest request = new ListAlertsRequest();

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        alertsApi.listAlerts(TEST_ACCOUNT_ID, request));

    assertEquals(500, exception.getHttpStatus());
    assertEquals("LIST_ALERTS_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("getAlertDetails - Error handling")
  void getAlertDetails_shouldHandleErrorResponse() throws Exception {
    String errorJson = "{\"Error\":{\"code\":\"11\",\"message\":\"Requested alert does not exist\"}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorJson);

    GetAlertDetailsRequest request = new GetAlertDetailsRequest();
    request.setId("99999");

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        alertsApi.getAlertDetails(TEST_ACCOUNT_ID, request));

    assertEquals(500, exception.getHttpStatus());
    assertEquals("GET_ALERT_DETAILS_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("deleteAlerts - Error handling")
  void deleteAlerts_shouldHandleErrorResponse() throws Exception {
    String errorJson = "{\"Error\":{\"code\":\"499\",\"message\":\"Alert Id should be greater than 0\"}}";

    when(apiClient.makeRequest(eq("DELETE"), anyString(), isNull(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(errorJson);

    DeleteAlertsRequest request = new DeleteAlertsRequest();
    request.setAlertIds(Arrays.asList("0"));

    EtradeApiException exception = assertThrows(EtradeApiException.class, () ->
        alertsApi.deleteAlerts(TEST_ACCOUNT_ID, request));

    assertEquals(500, exception.getHttpStatus());
    assertEquals("DELETE_ALERTS_FAILED", exception.getErrorCode());
  }

  @Test
  @DisplayName("listAlerts - Handles empty alerts")
  void listAlerts_shouldHandleEmptyAlerts() throws Exception {
    String responseJson = "{\"AlertsResponse\":{\"totalAlerts\":0,\"Alert\":[]}}";

    when(apiClient.makeRequest(eq("GET"), anyString(), any(), isNull(), eq(TEST_ACCOUNT_ID)))
        .thenReturn(responseJson);

    ListAlertsRequest request = new ListAlertsRequest();

    AlertsResponse result = alertsApi.listAlerts(TEST_ACCOUNT_ID, request);

    assertNotNull(result);
    assertEquals(0L, result.getTotalAlerts());
    assertNotNull(result.getAlerts());
    assertEquals(0, result.getAlerts().size());
  }
}
