package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PortfolioSnapshotDto(
    UUID id,
    UUID accountId,
    LocalDate asOfDate,
    BigDecimal equity,
    BigDecimal cash,
    BigDecimal pnl,
    BigDecimal drawdown,
    OffsetDateTime createdAt) {}
