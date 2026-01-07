package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderSource;
import com.myqyl.aitradex.domain.OrderStatus;
import com.myqyl.aitradex.domain.OrderType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDto(
    UUID id,
    UUID accountId,
    String symbol,
    OrderSide side,
    OrderType type,
    OrderStatus status,
    OrderSource source,
    BigDecimal limitPrice,
    BigDecimal stopPrice,
    BigDecimal quantity,
    OffsetDateTime routedAt,
    OffsetDateTime filledAt,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
