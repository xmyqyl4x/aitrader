package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePortfolioSnapshotRequest(
    @NotNull UUID accountId,
    @NotNull LocalDate asOfDate,
    @NotNull BigDecimal equity,
    @NotNull BigDecimal cash,
    BigDecimal pnl,
    BigDecimal drawdown) {}
