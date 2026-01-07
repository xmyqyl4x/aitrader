package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.ActorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuditLogRequest(
    @NotBlank String actor,
    @NotNull ActorType actorType,
    @NotBlank String action,
    @NotBlank String entityRef,
    String beforeState,
    String afterState) {}
