package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.etrade.service.EtradeAlertsService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE alerts operations.
 */
@RestController
@RequestMapping("/api/etrade/alerts")
@ConditionalOnProperty(name = "app.etrade.enabled", havingValue = "true", matchIfMissing = false)
public class EtradeAlertsController {

  private final EtradeAlertsService alertsService;

  public EtradeAlertsController(EtradeAlertsService alertsService) {
    this.alertsService = alertsService;
  }

  /**
   * Lists alerts for an account.
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listAlerts(
      @RequestParam UUID accountId,
      @RequestParam(required = false) Integer count,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String search) {
    List<Map<String, Object>> alerts = alertsService.listAlerts(accountId, count, category, 
                                                                  status, direction, search);
    return ResponseEntity.ok(alerts);
  }

  /**
   * Gets alert details by alert ID.
   */
  @GetMapping("/{alertId}")
  public ResponseEntity<Map<String, Object>> getAlertDetails(
      @RequestParam UUID accountId,
      @PathVariable String alertId,
      @RequestParam(required = false) Boolean htmlTags) {
    Map<String, Object> details = alertsService.getAlertDetails(accountId, alertId, htmlTags);
    return ResponseEntity.ok(details);
  }

  /**
   * Deletes one or more alerts.
   */
  @DeleteMapping
  public ResponseEntity<Map<String, Object>> deleteAlerts(
      @RequestParam UUID accountId,
      @RequestBody List<String> alertIds) {
    Map<String, Object> result = alertsService.deleteAlerts(accountId, alertIds);
    return ResponseEntity.ok(result);
  }
}
