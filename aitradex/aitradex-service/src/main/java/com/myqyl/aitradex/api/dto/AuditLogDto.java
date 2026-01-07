package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.ActorType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogDto(
    UUID id,
    String actor,
    ActorType actorType,
    String action,
    String entityRef,
    String beforeState,
    String afterState,
    OffsetDateTime occurredAt) {}
