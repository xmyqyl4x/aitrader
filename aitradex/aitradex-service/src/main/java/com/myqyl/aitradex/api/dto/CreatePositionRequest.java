package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePositionRequest(
    @NotNull UUID accountId,
    @NotBlank String symbol,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal costBasis,
    BigDecimal stopLoss,
    @NotNull OffsetDateTime openedAt) {}
