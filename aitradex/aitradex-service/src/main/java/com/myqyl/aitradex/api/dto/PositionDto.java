package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PositionDto(
    UUID id,
    UUID accountId,
    String symbol,
    BigDecimal quantity,
    BigDecimal costBasis,
    BigDecimal stopLoss,
    OffsetDateTime openedAt,
    OffsetDateTime closedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
