package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EtradeOrderDto(
    UUID id,
    UUID accountId,
    String etradeOrderId,
    String symbol,
    String orderType,
    String priceType,
    String side,
    Integer quantity,
    BigDecimal limitPrice,
    BigDecimal stopPrice,
    String orderStatus,
    OffsetDateTime placedAt,
    OffsetDateTime executedAt,
    OffsetDateTime cancelledAt,
    Map<String, Object> previewData,
    Map<String, Object> orderResponse) {}
