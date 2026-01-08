package com.myqyl.aitradex.etrade.service;

import com.myqyl.aitradex.etrade.client.EtradeAlertsClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for E*TRADE alerts operations.
 */
@Service
public class EtradeAlertsService {

  private final EtradeAlertsClient alertsClient;

  public EtradeAlertsService(EtradeAlertsClient alertsClient) {
    this.alertsClient = alertsClient;
  }

  /**
   * Lists alerts for an account.
   */
  public List<Map<String, Object>> listAlerts(UUID accountId, Integer count, String category,
                                               String status, String direction, String search) {
    return alertsClient.listAlerts(accountId, count, category, status, direction, search);
  }

  /**
   * Gets alert details by alert ID.
   */
  public Map<String, Object> getAlertDetails(UUID accountId, String alertId, Boolean htmlTags) {
    return alertsClient.getAlertDetails(accountId, alertId, htmlTags);
  }

  /**
   * Deletes one or more alerts.
   */
  public Map<String, Object> deleteAlerts(UUID accountId, List<String> alertIds) {
    return alertsClient.deleteAlerts(accountId, alertIds);
  }
}
