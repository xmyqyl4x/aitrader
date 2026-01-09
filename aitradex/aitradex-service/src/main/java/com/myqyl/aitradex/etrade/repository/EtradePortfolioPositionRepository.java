package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradePortfolioPosition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for E*TRADE Portfolio Positions.
 */
@Repository
public interface EtradePortfolioPositionRepository extends JpaRepository<EtradePortfolioPosition, UUID> {

  /**
   * Finds position by account ID and position ID (unique combination).
   */
  Optional<EtradePortfolioPosition> findByAccountIdAndPositionId(UUID accountId, Long positionId);

  /**
   * Finds all positions for an account, ordered by snapshot time descending (most recent first).
   */
  List<EtradePortfolioPosition> findByAccountIdOrderBySnapshotTimeDesc(UUID accountId);

  /**
   * Finds all positions for an account at a specific snapshot time.
   */
  List<EtradePortfolioPosition> findByAccountIdAndSnapshotTimeOrderBySymbol(UUID accountId, java.time.OffsetDateTime snapshotTime);

  /**
   * Counts positions for an account.
   */
  long countByAccountId(UUID accountId);

  /**
   * Deletes all positions for an account.
   */
  void deleteByAccountId(UUID accountId);
}
