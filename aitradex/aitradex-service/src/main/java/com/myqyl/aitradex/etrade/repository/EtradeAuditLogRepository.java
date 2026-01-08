package com.myqyl.aitradex.etrade.repository;

import com.myqyl.aitradex.etrade.domain.EtradeAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EtradeAuditLogRepository extends JpaRepository<EtradeAuditLog, UUID> {
  
  List<EtradeAuditLog> findByAccountId(UUID accountId);
  
  Page<EtradeAuditLog> findByAccountId(UUID accountId, Pageable pageable);
  
  List<EtradeAuditLog> findByAction(String action);
  
  Page<EtradeAuditLog> findByAccountIdAndAction(UUID accountId, String action, Pageable pageable);
}
