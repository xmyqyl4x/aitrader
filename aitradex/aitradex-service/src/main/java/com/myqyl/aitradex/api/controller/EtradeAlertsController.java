package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.service.EtradeAlertsService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E*TRADE alerts operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New endpoints use Alerts API DTOs.
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
   * Lists alerts for an account using DTOs.
   */
  @GetMapping
  public ResponseEntity<AlertsResponse> listAlerts(
      @RequestParam UUID accountId,
      @RequestParam(required = false) Integer count,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String search) {
    ListAlertsRequest request = new ListAlertsRequest();
    request.setCount(count);
    request.setCategory(category);
    request.setStatus(status);
    request.setDirection(direction);
    request.setSearch(search);
    
    AlertsResponse response = alertsService.listAlerts(accountId, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets alert details by alert ID using DTOs.
   */
  @GetMapping("/{alertId}")
  public ResponseEntity<AlertDetailsDto> getAlertDetails(
      @RequestParam UUID accountId,
      @PathVariable String alertId,
      @RequestParam(required = false) Boolean tags) {
    GetAlertDetailsRequest request = new GetAlertDetailsRequest();
    request.setId(alertId);
    request.setTags(tags);
    
    AlertDetailsDto details = alertsService.getAlertDetails(accountId, request);
    return ResponseEntity.ok(details);
  }

  /**
   * Deletes one or more alerts using DTOs.
   * Alert IDs are provided as a comma-separated path parameter per E*TRADE API documentation.
   */
  @DeleteMapping("/{alertIdList}")
  public ResponseEntity<DeleteAlertsResponse> deleteAlerts(
      @RequestParam UUID accountId,
      @PathVariable String alertIdList) {
    DeleteAlertsRequest request = new DeleteAlertsRequest();
    // Parse comma-separated alert IDs from path parameter
    String[] alertIds = alertIdList.split(",");
    request.setAlertIds(java.util.Arrays.asList(alertIds));
    
    DeleteAlertsResponse response = alertsService.deleteAlerts(accountId, request);
    return ResponseEntity.ok(response);
  }
}
