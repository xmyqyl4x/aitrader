package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeAlertEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for EtradeAlertEvent entity.
 */
@Repository
public interface EtradeAlertEventRepository extends JpaRepository<EtradeAlertEvent, UUID> {

  /**
   * Find all events for an alert.
   */
  List<EtradeAlertEvent> findByAlertId(UUID alertId);

  /**
   * Find events by alert ID and event type.
   */
  List<EtradeAlertEvent> findByAlertIdAndEventType(UUID alertId, String eventType);
}
