package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExecutionDto(
    UUID id,
    UUID orderId,
    BigDecimal price,
    BigDecimal quantity,
    String venue,
    OffsetDateTime executedAt) {}
