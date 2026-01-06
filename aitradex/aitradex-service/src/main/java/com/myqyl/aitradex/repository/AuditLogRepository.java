package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
