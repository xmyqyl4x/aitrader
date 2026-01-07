package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnalyticsSummaryDto(
    UUID accountId,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal startingEquity,
    BigDecimal endingEquity,
    BigDecimal absolutePnl,
    BigDecimal returnPct,
    BigDecimal maxDrawdown) {}
