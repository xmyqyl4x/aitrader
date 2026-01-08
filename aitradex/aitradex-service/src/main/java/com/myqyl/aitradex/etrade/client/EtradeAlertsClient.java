package com.myqyl.aitradex.etrade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import java.util.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for E*TRADE Alerts API endpoints.
 */
@Component
public class EtradeAlertsClient {

  private static final Logger log = LoggerFactory.getLogger(EtradeAlertsClient.class);

  private final EtradeApiClient apiClient;
  private final EtradeProperties properties;
  private final ObjectMapper objectMapper;

  public EtradeAlertsClient(EtradeApiClient apiClient, EtradeProperties properties, 
                            ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * Lists alerts for a user.
   * 
   * @param accountId Internal account UUID
   * @param count Number of alerts to return (optional)
   * @param category Alert category filter (optional)
   * @param status Alert status filter (optional)
   * @param direction Sort direction (optional)
   * @param search Search term (optional)
   * @return List of alerts
   */
  public List<Map<String, Object>> listAlerts(UUID accountId, Integer count, String category, 
                                                String status, String direction, String search) {
    try {
      String url = properties.getAlertsListUrl();
      Map<String, String> params = new HashMap<>();
      
      if (count != null && count > 0) {
        params.put("count", String.valueOf(count));
      }
      if (category != null && !category.isEmpty()) {
        params.put("category", category);
      }
      if (status != null && !status.isEmpty()) {
        params.put("status", status);
      }
      if (direction != null && !direction.isEmpty()) {
        params.put("direction", direction);
      }
      if (search != null && !search.isEmpty()) {
        params.put("search", search);
      }
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode alertsNode = root.path("AlertsResponse").path("Alert");
      
      List<Map<String, Object>> alerts = new ArrayList<>();
      if (alertsNode.isArray()) {
        for (JsonNode alertNode : alertsNode) {
          alerts.add(parseAlert(alertNode));
        }
      } else if (alertsNode.isObject()) {
        alerts.add(parseAlert(alertsNode));
      }
      
      return alerts;
    } catch (Exception e) {
      log.error("Failed to list alerts", e);
      throw new RuntimeException("Failed to list alerts", e);
    }
  }

  /**
   * Gets alert details by alert ID.
   * 
   * @param accountId Internal account UUID
   * @param alertId Alert ID
   * @param htmlTags Whether to include HTML tags (optional)
   * @return Alert details
   */
  public Map<String, Object> getAlertDetails(UUID accountId, String alertId, Boolean htmlTags) {
    try {
      String url = properties.getAlertDetailsUrl(alertId);
      Map<String, String> params = new HashMap<>();
      
      if (htmlTags != null) {
        params.put("htmlTags", String.valueOf(htmlTags));
      }
      
      String response = apiClient.makeRequest("GET", url, params, null, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode alertNode = root.path("AlertDetailsResponse").path("Alert");
      
      return parseAlertDetails(alertNode);
    } catch (Exception e) {
      log.error("Failed to get alert details for alert {}", alertId, e);
      throw new RuntimeException("Failed to get alert details", e);
    }
  }

  /**
   * Deletes one or more alerts.
   * 
   * @param accountId Internal account UUID
   * @param alertIds List of alert IDs to delete
   * @return Deletion result
   */
  public Map<String, Object> deleteAlerts(UUID accountId, List<String> alertIds) {
    try {
      String url = properties.getAlertsDeleteUrl();
      
      // Build request body with alert IDs
      Map<String, Object> requestBody = new HashMap<>();
      Map<String, Object> deleteRequest = new HashMap<>();
      deleteRequest.put("alertId", alertIds);
      requestBody.put("DeleteAlertRequest", deleteRequest);
      
      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      
      String response = apiClient.makeRequest("DELETE", url, null, requestBodyJson, accountId);
      
      JsonNode root = objectMapper.readTree(response);
      JsonNode deleteResponse = root.path("DeleteAlertResponse");
      
      return parseDeleteResponse(deleteResponse);
    } catch (Exception e) {
      log.error("Failed to delete alerts", e);
      throw new RuntimeException("Failed to delete alerts", e);
    }
  }

  private Map<String, Object> parseAlert(JsonNode alertNode) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("id", alertNode.path("id").asText(""));
    alert.put("subject", alertNode.path("subject").asText(""));
    alert.put("dateTime", alertNode.path("dateTime").asText(""));
    alert.put("category", alertNode.path("category").asText(""));
    alert.put("status", alertNode.path("status").asText(""));
    alert.put("priority", alertNode.path("priority").asText(""));
    
    // Additional optional fields
    JsonNode readNode = alertNode.path("read");
    if (!readNode.isMissingNode()) {
      alert.put("read", readNode.asBoolean(false));
    }
    alert.put("readDate", alertNode.path("readDate").asText(""));
    alert.put("url", alertNode.path("url").asText(""));
    alert.put("alertType", alertNode.path("alertType").asText(""));
    alert.put("accountId", alertNode.path("accountId").asText(""));
    
    return alert;
  }

  private Map<String, Object> parseAlertDetails(JsonNode alertNode) {
    Map<String, Object> details = new HashMap<>();
    details.put("id", alertNode.path("id").asText(""));
    details.put("subject", alertNode.path("subject").asText(""));
    details.put("dateTime", alertNode.path("dateTime").asText(""));
    details.put("category", alertNode.path("category").asText(""));
    details.put("status", alertNode.path("status").asText(""));
    details.put("priority", alertNode.path("priority").asText(""));
    details.put("message", alertNode.path("message").asText(""));
    
    // Additional optional fields
    JsonNode readNode = alertNode.path("read");
    if (!readNode.isMissingNode()) {
      details.put("read", readNode.asBoolean(false));
    }
    details.put("readDate", alertNode.path("readDate").asText(""));
    details.put("url", alertNode.path("url").asText(""));
    details.put("alertType", alertNode.path("alertType").asText(""));
    details.put("accountId", alertNode.path("accountId").asText(""));
    details.put("htmlMessage", alertNode.path("htmlMessage").asText(""));
    
    return details;
  }

  private Map<String, Object> parseDeleteResponse(JsonNode deleteResponse) {
    Map<String, Object> result = new HashMap<>();
    result.put("success", deleteResponse.path("success").asBoolean(false));
    result.put("message", deleteResponse.path("message").asText(""));
    return result;
  }
}
