package com.myqyl.aitradex.etrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.etrade.alerts.dto.*;
import com.myqyl.aitradex.etrade.client.EtradeAlertsClient;
import com.myqyl.aitradex.etrade.client.EtradeApiClientAlertsAPI;
import com.myqyl.aitradex.etrade.domain.EtradeAlert;
import com.myqyl.aitradex.etrade.domain.EtradeAlertDetail;
import com.myqyl.aitradex.etrade.domain.EtradeAlertEvent;
import com.myqyl.aitradex.etrade.repository.EtradeAlertDetailRepository;
import com.myqyl.aitradex.etrade.repository.EtradeAlertEventRepository;
import com.myqyl.aitradex.etrade.repository.EtradeAlertRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for E*TRADE alerts operations.
 * 
 * Refactored to use DTOs/Models instead of Maps.
 * New methods use EtradeApiClientAlertsAPI with DTOs.
 * Includes database persistence for alerts, alert details, and alert events.
 */
@Service
public class EtradeAlertsService {

  private static final Logger log = LoggerFactory.getLogger(EtradeAlertsService.class);

  private final EtradeAlertsClient alertsClient; // Deprecated - use alertsApi instead
  private final EtradeApiClientAlertsAPI alertsApi; // New API client with DTOs
  private final EtradeAlertRepository alertRepository;
  private final EtradeAlertDetailRepository alertDetailRepository;
  private final EtradeAlertEventRepository alertEventRepository;

  public EtradeAlertsService(
      EtradeAlertsClient alertsClient,
      EtradeApiClientAlertsAPI alertsApi,
      EtradeAlertRepository alertRepository,
      EtradeAlertDetailRepository alertDetailRepository,
      EtradeAlertEventRepository alertEventRepository) {
    this.alertsClient = alertsClient;
    this.alertsApi = alertsApi;
    this.alertRepository = alertRepository;
    this.alertDetailRepository = alertDetailRepository;
    this.alertEventRepository = alertEventRepository;
  }

  /**
   * Lists alerts for an account using DTOs and persists alerts to database.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request ListAlertsRequest DTO containing query parameters
   * @return AlertsResponse DTO containing totalAlerts and alerts list
   */
  @Transactional(noRollbackFor = com.myqyl.aitradex.etrade.exception.EtradeApiException.class)
  public AlertsResponse listAlerts(UUID accountId, ListAlertsRequest request) {
    AlertsResponse response = alertsApi.listAlerts(accountId, request);
    
    // Persist alerts (upsert by accountId + alertId)
    persistAlerts(accountId, response);
    
    return response;
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
   * Gets alert details by alert ID using DTOs and persists details to database.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request GetAlertDetailsRequest DTO containing alert ID and tags parameter
   * @return AlertDetailsDto DTO containing alert details
   */
  @Transactional(noRollbackFor = com.myqyl.aitradex.etrade.exception.EtradeApiException.class)
  public AlertDetailsDto getAlertDetails(UUID accountId, GetAlertDetailsRequest request) {
    AlertDetailsDto details = alertsApi.getAlertDetails(accountId, request);
    
    // Persist alert details (upsert by alertId)
    persistAlertDetails(accountId, details);
    
    return details;
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
   * Deletes one or more alerts using DTOs and updates database state.
   * 
   * @param accountId Internal account UUID for authentication
   * @param request DeleteAlertsRequest DTO containing list of alert IDs
   * @return DeleteAlertsResponse DTO containing result and failed alerts
   */
  @Transactional(noRollbackFor = com.myqyl.aitradex.etrade.exception.EtradeApiException.class)
  public DeleteAlertsResponse deleteAlerts(UUID accountId, DeleteAlertsRequest request) {
    DeleteAlertsResponse response = alertsApi.deleteAlerts(accountId, request);
    
    // Update database state for deleted alerts
    persistDeleteAlerts(accountId, request, response);
    
    return response;
  }

  /**
   * Deletes one or more alerts (deprecated - uses Maps).
   * @deprecated Use {@link #deleteAlerts(UUID, DeleteAlertsRequest)} instead
   */
  @Deprecated
  public Map<String, Object> deleteAlerts(UUID accountId, List<String> alertIds) {
    return alertsClient.deleteAlerts(accountId, alertIds);
  }

  // ============================================================================
  // Private persistence helper methods
  // ============================================================================

  /**
   * Persists alerts from List Alerts response (upsert by accountId + alertId).
   */
  private void persistAlerts(UUID accountId, AlertsResponse response) {
    if (response == null || response.getAlerts() == null) {
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();

    for (AlertDto alertDto : response.getAlerts()) {
      if (alertDto == null || alertDto.getId() == null) {
        continue;
      }

      Optional<EtradeAlert> existingAlert = alertRepository.findByAccountIdAndAlertId(accountId, alertDto.getId());
      
      EtradeAlert alert;
      if (existingAlert.isPresent()) {
        alert = existingAlert.get();
      } else {
        alert = new EtradeAlert();
        alert.setAccountId(accountId);
        alert.setAlertId(alertDto.getId());
      }

      // Update fields from DTO
      alert.setCreateTime(alertDto.getCreateTime());
      alert.setSubject(alertDto.getSubject());
      alert.setStatus(alertDto.getStatus());
      alert.setLastSyncedAt(now);

      alertRepository.save(alert);
    }

    log.debug("Persisted {} alerts for account {}", response.getAlerts().size(), accountId);
  }

  /**
   * Persists alert details from Get Alert Details response (upsert by alertId).
   */
  private void persistAlertDetails(UUID accountId, AlertDetailsDto details) {
    if (details == null || details.getId() == null) {
      return;
    }

    // First, find or create the alert record
    Optional<EtradeAlert> alertOpt = alertRepository.findByAccountIdAndAlertId(accountId, details.getId());
    EtradeAlert alert;
    
    if (alertOpt.isPresent()) {
      alert = alertOpt.get();
    } else {
      // Create alert record if it doesn't exist
      alert = new EtradeAlert();
      alert.setAccountId(accountId);
      alert.setAlertId(details.getId());
      alert.setCreateTime(details.getCreateTime());
      alert.setSubject(details.getSubject());
      alert.setStatus("UNREAD"); // Default status
      alert = alertRepository.save(alert);
    }

    // Update alert status if deleteTime > 0
    if (details.getDeleteTime() != null && details.getDeleteTime() > 0) {
      alert.setStatus("DELETED");
      alertRepository.save(alert);
    }

    // Upsert alert details
    Optional<EtradeAlertDetail> detailOpt = alertDetailRepository.findByAlertId(alert.getId());
    EtradeAlertDetail detail;
    
    if (detailOpt.isPresent()) {
      detail = detailOpt.get();
    } else {
      detail = new EtradeAlertDetail();
      detail.setAlertId(alert.getId());
    }

    // Update detail fields
    detail.setMsgText(details.getMsgText());
    detail.setReadTime(details.getReadTime());
    detail.setDeleteTime(details.getDeleteTime());
    detail.setSymbol(details.getSymbol());
    detail.setNextUrl(details.getNext());
    detail.setPrevUrl(details.getPrev());
    detail.setDetailsFetchedAt(OffsetDateTime.now());

    alertDetailRepository.save(detail);

    log.debug("Persisted alert details for alert {}", details.getId());
  }

  /**
   * Persists delete alerts response and updates database state.
   */
  private void persistDeleteAlerts(UUID accountId, DeleteAlertsRequest request, DeleteAlertsResponse response) {
    if (request == null || request.getAlertIds() == null || response == null) {
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();

    // Process successfully deleted alerts
    for (String alertIdStr : request.getAlertIds()) {
      try {
        Long alertId = Long.parseLong(alertIdStr);
        Optional<EtradeAlert> alertOpt = alertRepository.findByAccountIdAndAlertId(accountId, alertId);
        
        if (alertOpt.isPresent()) {
          EtradeAlert alert = alertOpt.get();
          
          // Check if this alert ID was in the failed list
          boolean failed = response.getFailedAlerts() != null &&
              response.getFailedAlerts().stream()
                  .anyMatch(failedAlert -> failedAlert.getId() != null && failedAlert.getId().equals(alertId));
          
          if (!failed) {
            // Successfully deleted - update status
            alert.setStatus("DELETED");
            alert.setLastSyncedAt(now);
            alertRepository.save(alert);
            
            // Create success event
            createAlertEvent(alert.getId(), "DELETE_SUCCESS", "SUCCESS", null, null);
          } else {
            // Failed to delete - create failure event
            String reason = response.getFailedAlerts().stream()
                .filter(failedAlert -> failedAlert.getId() != null && failedAlert.getId().equals(alertId))
                .findFirst()
                .map(FailedAlertDto::getReason)
                .orElse("Unknown reason");
            
            createAlertEvent(alert.getId(), "DELETE_FAILURE", "FAILURE", reason, null);
          }
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid alert ID format: {}", alertIdStr);
      }
    }

    log.debug("Processed delete alerts response for account {}", accountId);
  }

  /**
   * Creates an alert event record for auditability.
   */
  private void createAlertEvent(UUID alertId, String eventType, String eventStatus, String errorMessage, String eventData) {
    EtradeAlertEvent event = new EtradeAlertEvent();
    event.setAlertId(alertId);
    event.setEventType(eventType);
    event.setEventStatus(eventStatus);
    event.setErrorMessage(errorMessage);
    event.setEventData(eventData);
    
    alertEventRepository.save(event);
  }
}
