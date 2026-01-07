package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarketDataQuoteDto(
    String symbol,
    OffsetDateTime asOf,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume,
    String source) {}
