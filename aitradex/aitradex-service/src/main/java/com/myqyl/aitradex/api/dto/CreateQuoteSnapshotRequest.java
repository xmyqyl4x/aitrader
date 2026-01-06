package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateQuoteSnapshotRequest(
    @NotBlank String symbol,
    @NotNull OffsetDateTime asOf,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume,
    String source) {}
