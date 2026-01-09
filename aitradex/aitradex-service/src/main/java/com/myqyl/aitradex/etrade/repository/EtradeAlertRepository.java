package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeAlert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for EtradeAlert entity.
 */
@Repository
public interface EtradeAlertRepository extends JpaRepository<EtradeAlert, UUID> {

  /**
   * Find alert by account ID and E*TRADE alert ID (for upsert logic).
   */
  Optional<EtradeAlert> findByAccountIdAndAlertId(UUID accountId, Long alertId);

  /**
   * Find all alerts for an account.
   */
  List<EtradeAlert> findByAccountId(UUID accountId);

  /**
   * Find alerts by account ID and status.
   */
  List<EtradeAlert> findByAccountIdAndStatus(UUID accountId, String status);
}
