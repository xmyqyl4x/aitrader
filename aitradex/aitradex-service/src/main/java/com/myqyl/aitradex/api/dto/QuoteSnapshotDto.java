package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuoteSnapshotDto(
    UUID id,
    String symbol,
    OffsetDateTime asOf,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume,
    String source,
    OffsetDateTime createdAt) {}
