package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import com.myqyl.aitradex.etrade.exception.EtradeApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E*TRADE Alerts API Client.
 * 
 * This class refactors alerts-specific functionality from EtradeAlertsClient
 * into a dedicated Alerts API layer.
 * 
 * Implements all 3 Alerts API endpoints as per E*TRADE Alerts API documentation:
 * 1. List Alerts
 * 2. Get Alert Details
 * 3. Delete Alerts
 * 
 * All request and response objects are DTOs/Models, not Maps, as per requirements.
 */
@Component
public class EtradeApiClientAlertsAPI {

  private static final Logger log = LoggerFactory.getLogger(EtradeApiClientAlertsAPI.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeApiClientAlertsAPI(
      EtradeApiClient apiClient,
      EtradeProperties properties,
      ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * 1. List Alerts
   * 
   * Provides a list of alerts for the user.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request ListAlertsRequest DTO containing query parameters
   * @return AlertsResponse DTO containing totalAlerts and alerts list
   * @throws EtradeApiException if the request fails
   */
  public AlertsResponse listAlerts(UUID accountId, ListAlertsRequest request) {
    try {
      String url = properties.getAlertsListUrl();
      Map<String, String> params = new HashMap<>();
      
      // Add optional query parameters
      if (request.getCount() != null && request.getCount() > 0) {
        params.put("count", String.valueOf(request.getCount()));
      }
      if (request.getCategory() != null && !request.getCategory().isEmpty()) {
        params.put("category", request.getCategory());
      }
      if (request.getStatus() != null && !request.getStatus().isEmpty()) {
        params.put("status", request.getStatus());
      }
      if (request.getDirection() != null && !request.getDirection().isEmpty()) {
        params.put("direction", request.getDirection());
      }
      if (request.getSearch() != null && !request.getSearch().isEmpty()) {
        params.put("search", request.getSearch());
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode alertsResponseNode = root.path("AlertsResponse");
      
      return parseAlertsResponse(alertsResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list alerts", e);
      throw new EtradeApiException(500, "LIST_ALERTS_FAILED", 
          "Failed to list alerts: " + e.getMessage(), e);
    }
  }

  /**
   * 2. Get Alert Details
   * 
   * Provides details for an alert.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request GetAlertDetailsRequest DTO containing alert ID and tags parameter
   * @return AlertDetailsDto DTO containing alert details
   * @throws EtradeApiException if the request fails
   */
  public AlertDetailsDto getAlertDetails(UUID accountId, GetAlertDetailsRequest request) {
    try {
      String url = properties.getAlertDetailsUrl(request.getId());
      Map<String, String> params = new HashMap<>();
      
      // Add optional query parameter
      if (request.getTags() != null) {
        params.put("tags", String.valueOf(request.getTags()));
      }
      
      String response = apiClient.makeRequest("GET", url, params.isEmpty() ? null : params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode alertDetailsNode = root.path("AlertDetailsResponse");
      
      return parseAlertDetails(alertDetailsNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get alert details for alert {}", request.getId(), e);
      throw new EtradeApiException(500, "GET_ALERT_DETAILS_FAILED", 
          "Failed to get alert details: " + e.getMessage(), e);
    }
  }

  /**
   * 3. Delete Alerts
   * 
   * Deletes a list of alerts.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request DeleteAlertsRequest DTO containing list of alert IDs
   * @return DeleteAlertsResponse DTO containing result and failed alerts
   * @throws EtradeApiException if the request fails
   */
  public DeleteAlertsResponse deleteAlerts(UUID accountId, DeleteAlertsRequest request) {
    try {
      // Build URL with comma-separated alert IDs in path (per E*TRADE API documentation)
      String alertIdList = request.toPathParameter();
      String url = properties.getAlertsDeleteUrl(alertIdList);
      
      // DELETE request with no body (alert IDs are in path)
      String response = apiClient.makeRequest("DELETE", url, null, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode deleteResponseNode = root.path("AlertsResponse"); // Note: E*TRADE returns "AlertsResponse" for delete
      
      return parseDeleteAlertsResponse(deleteResponseNode);
    } catch (EtradeApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to delete alerts", e);
      throw new EtradeApiException(500, "DELETE_ALERTS_FAILED", 
          "Failed to delete alerts: " + e.getMessage(), e);
    }
  }

  // ============================================================================
  // Response Parsers (convert JSON to DTOs)
  // ============================================================================

  /**
   * Parses AlertsResponse JSON node into AlertsResponse DTO.
   */
  private AlertsResponse parseAlertsResponse(JsonNode alertsResponseNode) {
    AlertsResponse response = new AlertsResponse();
    
    // Parse totalAlerts
    response.setTotalAlerts(getLongValue(alertsResponseNode, "totalAlerts"));
    
    // Parse Alert array
    JsonNode alertsArrayNode = alertsResponseNode.path("Alert");
    List<AlertDto> alertsList = new ArrayList<>();
    if (alertsArrayNode.isArray()) {
      for (JsonNode alertNode : alertsArrayNode) {
        alertsList.add(parseAlert(alertNode));
      }
    } else if (alertsArrayNode.isObject() && !alertsArrayNode.isMissingNode()) {
      alertsList.add(parseAlert(alertsArrayNode));
    }
    response.setAlerts(alertsList);
    
    return response;
  }

  /**
   * Parses Alert JSON node into AlertDto DTO.
   */
  private AlertDto parseAlert(JsonNode alertNode) {
    AlertDto alert = new AlertDto();
    alert.setId(getLongValue(alertNode, "id"));
    alert.setCreateTime(getLongValue(alertNode, "createTime"));
    alert.setSubject(getStringValue(alertNode, "subject"));
    alert.setStatus(getStringValue(alertNode, "status"));
    return alert;
  }

  /**
   * Parses AlertDetailsResponse JSON node into AlertDetailsDto DTO.
   */
  private AlertDetailsDto parseAlertDetails(JsonNode alertDetailsNode) {
    AlertDetailsDto details = new AlertDetailsDto();
    
    details.setId(getLongValue(alertDetailsNode, "id"));
    details.setCreateTime(getLongValue(alertDetailsNode, "createTime"));
    details.setSubject(getStringValue(alertDetailsNode, "subject"));
    details.setMsgText(getStringValue(alertDetailsNode, "msgText"));
    details.setReadTime(getLongValue(alertDetailsNode, "readTime"));
    details.setDeleteTime(getLongValue(alertDetailsNode, "deleteTime"));
    details.setSymbol(getStringValue(alertDetailsNode, "symbol"));
    details.setNext(getStringValue(alertDetailsNode, "next"));
    details.setPrev(getStringValue(alertDetailsNode, "prev"));
    
    return details;
  }

  /**
   * Parses DeleteAlertsResponse JSON node into DeleteAlertsResponse DTO.
   * Note: E*TRADE API returns "AlertsResponse" with "result" field for delete operations.
   */
  private DeleteAlertsResponse parseDeleteAlertsResponse(JsonNode deleteResponseNode) {
    DeleteAlertsResponse response = new DeleteAlertsResponse();
    
    // Parse result field
    response.setResult(getStringValue(deleteResponseNode, "result"));
    
    // Parse FailedAlerts array if present
    JsonNode failedAlertsNode = deleteResponseNode.path("FailedAlerts");
    if (!failedAlertsNode.isMissingNode()) {
      List<FailedAlertDto> failedAlerts = new ArrayList<>();
      if (failedAlertsNode.isArray()) {
        for (JsonNode failedAlertNode : failedAlertsNode) {
          FailedAlertDto failedAlert = new FailedAlertDto();
          failedAlert.setId(getLongValue(failedAlertNode, "id"));
          failedAlert.setReason(getStringValue(failedAlertNode, "reason"));
          failedAlerts.add(failedAlert);
        }
      } else if (failedAlertsNode.isObject()) {
        FailedAlertDto failedAlert = new FailedAlertDto();
        failedAlert.setId(getLongValue(failedAlertsNode, "id"));
        failedAlert.setReason(getStringValue(failedAlertsNode, "reason"));
        failedAlerts.add(failedAlert);
      }
      response.setFailedAlerts(failedAlerts);
    }
    
    return response;
  }

  // ============================================================================
  // Helper methods for parsing JSON values
  // ============================================================================

  private String getStringValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    String text = fieldNode.asText();
    return (text != null && !text.isEmpty()) ? text : null;
  }

  private Long getLongValue(JsonNode node, String fieldName) {
    JsonNode fieldNode = node.path(fieldName);
    if (fieldNode.isMissingNode() || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asLong();
    }
    try {
      String text = fieldNode.asText();
      if (text == null || text.isEmpty()) {
        return null;
      }
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
