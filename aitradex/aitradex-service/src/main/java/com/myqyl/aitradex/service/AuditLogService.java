package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.AuditLogDto;
import com.myqyl.aitradex.api.dto.CreateAuditLogRequest;
import com.myqyl.aitradex.domain.AuditLog;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public AuditLogDto create(CreateAuditLogRequest request) {
    AuditLog log =
        AuditLog.builder()
            .actor(request.actor())
            .actorType(request.actorType())
            .action(request.action())
            .entityRef(request.entityRef())
            .beforeState(request.beforeState())
            .afterState(request.afterState())
            .occurredAt(OffsetDateTime.now())
            .build();
    return toDto(auditLogRepository.save(log));
  }

  @Transactional(readOnly = true)
  public List<AuditLogDto> list(String entityRef) {
    List<AuditLog> logs =
        entityRef != null
            ? auditLogRepository.findByEntityRefOrderByOccurredAtDesc(entityRef)
            : auditLogRepository.findAllByOrderByOccurredAtDesc();
    return logs.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public AuditLogDto get(UUID id) {
    return auditLogRepository.findById(id).map(this::toDto).orElseThrow(() -> auditNotFound(id));
  }

  private AuditLogDto toDto(AuditLog log) {
    return new AuditLogDto(
        log.getId(),
        log.getActor(),
        log.getActorType(),
        log.getAction(),
        log.getEntityRef(),
        log.getBeforeState(),
        log.getAfterState(),
        log.getOccurredAt());
  }

  private NotFoundException auditNotFound(UUID id) {
    return new NotFoundException("Audit log %s not found".formatted(id));
  }
}
