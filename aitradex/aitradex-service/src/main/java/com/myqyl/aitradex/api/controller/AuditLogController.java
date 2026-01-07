package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.AuditLogDto;
import com.myqyl.aitradex.api.dto.CreateAuditLogRequest;
import com.myqyl.aitradex.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AuditLogDto create(@Valid @RequestBody CreateAuditLogRequest request) {
    return auditLogService.create(request);
  }

  @GetMapping
  public List<AuditLogDto> list(
      @RequestParam(value = "entityRef", required = false) String entityRef,
      @RequestParam(value = "actor", required = false) String actor) {
    if (actor != null && !actor.isBlank()) {
      return auditLogService.listByActor(actor);
    }
    return auditLogService.list(entityRef);
  }

  @GetMapping("/{id}")
  public AuditLogDto get(@PathVariable UUID id) {
    return auditLogService.get(id);
  }
}
