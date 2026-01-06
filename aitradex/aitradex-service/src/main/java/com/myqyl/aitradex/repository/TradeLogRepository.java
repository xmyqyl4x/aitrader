package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.TradeLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeLogRepository extends JpaRepository<TradeLog, UUID> {
}
