package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;

public record SymbolPnlDto(
    String symbol,
    BigDecimal quantity,
    BigDecimal costBasis,
    BigDecimal lastPrice,
    BigDecimal unrealizedPnl) {}
