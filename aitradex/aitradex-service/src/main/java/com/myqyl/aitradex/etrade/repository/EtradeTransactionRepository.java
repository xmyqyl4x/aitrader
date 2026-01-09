package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeTransaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for E*TRADE Transactions.
 */
@Repository
public interface EtradeTransactionRepository extends JpaRepository<EtradeTransaction, UUID> {

  /**
   * Finds transaction by transaction ID (unique).
   */
  Optional<EtradeTransaction> findByTransactionId(String transactionId);

  /**
   * Finds all transactions for an account, ordered by transaction date descending (most recent first).
   */
  List<EtradeTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

  /**
   * Counts transactions for an account.
   */
  long countByAccountId(UUID accountId);

  /**
   * Deletes all transactions for an account.
   */
  void deleteByAccountId(UUID accountId);
}
