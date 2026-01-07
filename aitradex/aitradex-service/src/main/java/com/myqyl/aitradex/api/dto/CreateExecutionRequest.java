package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateExecutionRequest(
    @NotNull UUID orderId,
    @NotNull BigDecimal price,
    @NotNull BigDecimal quantity,
    String venue,
    OffsetDateTime executedAt) {}
