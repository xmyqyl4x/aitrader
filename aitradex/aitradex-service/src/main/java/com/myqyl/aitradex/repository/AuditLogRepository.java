package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  List<AuditLog> findByEntityRefOrderByOccurredAtDesc(String entityRef);

  List<AuditLog> findAllByOrderByOccurredAtDesc();

  List<AuditLog> findByActorOrderByOccurredAtDesc(String actor);
}
