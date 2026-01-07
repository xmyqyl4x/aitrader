package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.TradeLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeLogRepository extends JpaRepository<TradeLog, UUID> {

  List<TradeLog> findByAccountIdOrderByOccurredAtDesc(UUID accountId);

  List<TradeLog> findAllByOrderByOccurredAtDesc();
}
