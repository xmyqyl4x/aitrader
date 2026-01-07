package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateStockQuoteSearchRequest(
    @NotBlank String symbol,
    String companyName,
    String exchange,
    @NotBlank String range,
    @NotNull OffsetDateTime requestedAt,
    String status,
    OffsetDateTime quoteTimestamp,
    BigDecimal price,
    String currency,
    BigDecimal changeAmount,
    BigDecimal changePercent,
    Long volume,
    @NotBlank String provider,
    String requestId,
    String correlationId,
    String errorCode,
    String errorMessage,
    Integer durationMs) {}
