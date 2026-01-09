package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeAlertsClient;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAlertsAPI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for E*TRADE alerts operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New methods use EtradeApiClientAlertsAPI with DTOs.
 * Old methods are deprecated and delegate to new methods where possible.
 */
@Service
public class EtradeAlertsService {

  private final EtradeAlertsClient alertsClient; // Deprecated - use alertsApi instead
  private final EtradeApiClientAlertsAPI alertsApi; // New API client with DTOs

  public EtradeAlertsService(EtradeAlertsClient alertsClient, EtradeApiClientAlertsAPI alertsApi) {
    this.alertsClient = alertsClient;
    this.alertsApi = alertsApi;
  }

  /**
   * Lists alerts for an account using DTOs.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request ListAlertsRequest DTO containing query parameters
   * @return AlertsResponse DTO containing totalAlerts and alerts list
   */
  public AlertsResponse listAlerts(UUID accountId, ListAlertsRequest request) {
    return alertsApi.listAlerts(accountId, request);
  }

  /**
   * Lists alerts for an account (deprecated - uses Maps).
   * @deprecated Use {@link #listAlerts(UUID, ListAlertsRequest)} instead
   */
  @Deprecated
  public List<Map<String, Object>> listAlerts(UUID accountId, Integer count, String category,
                                               String status, String direction, String search) {
    return alertsClient.listAlerts(accountId, count, category, status, direction, search);
  }

  /**
   * Gets alert details by alert ID using DTOs.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request GetAlertDetailsRequest DTO containing alert ID and tags parameter
   * @return AlertDetailsDto DTO containing alert details
   */
  public AlertDetailsDto getAlertDetails(UUID accountId, GetAlertDetailsRequest request) {
    return alertsApi.getAlertDetails(accountId, request);
  }

  /**
   * Gets alert details by alert ID (deprecated - uses Maps).
   * @deprecated Use {@link #getAlertDetails(UUID, GetAlertDetailsRequest)} instead
   */
  @Deprecated
  public Map<String, Object> getAlertDetails(UUID accountId, String alertId, Boolean htmlTags) {
    return alertsClient.getAlertDetails(accountId, alertId, htmlTags);
  }

  /**
   * Deletes one or more alerts using DTOs.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request DeleteAlertsRequest DTO containing list of alert IDs
   * @return DeleteAlertsResponse DTO containing result and failed alerts
   */
  public DeleteAlertsResponse deleteAlerts(UUID accountId, DeleteAlertsRequest request) {
    return alertsApi.deleteAlerts(accountId, request);
  }

  /**
   * Deletes one or more alerts (deprecated - uses Maps).
   * @deprecated Use {@link #deleteAlerts(UUID, DeleteAlertsRequest)} instead
   */
  @Deprecated
  public Map<String, Object> deleteAlerts(UUID accountId, List<String> alertIds) {
    return alertsClient.deleteAlerts(accountId, alertIds);
  }
}
