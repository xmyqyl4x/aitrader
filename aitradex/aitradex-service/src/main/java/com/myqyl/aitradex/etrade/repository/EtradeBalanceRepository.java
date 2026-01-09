package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeBalance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for E*TRADE Balance snapshots.
 */
@Repository
public interface EtradeBalanceRepository extends JpaRepository<EtradeBalance, UUID> {

  /**
   * Finds all balance snapshots for an account, ordered by snapshot time descending (most recent first).
   */
  List<EtradeBalance> findByAccountIdOrderBySnapshotTimeDesc(UUID accountId);

  /**
   * Counts balance snapshots for an account.
   */
  long countByAccountId(UUID accountId);

  /**
   * Deletes all balance snapshots for an account.
   */
  void deleteByAccountId(UUID accountId);
}
