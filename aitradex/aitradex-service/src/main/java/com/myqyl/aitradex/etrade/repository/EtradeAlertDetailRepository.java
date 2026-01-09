package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeAlertDetail;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for EtradeAlertDetail entity.
 */
@Repository
public interface EtradeAlertDetailRepository extends JpaRepository<EtradeAlertDetail, UUID> {

  /**
   * Find alert detail by alert ID (one-to-one relationship).
   */
  Optional<EtradeAlertDetail> findByAlertId(UUID alertId);
}
