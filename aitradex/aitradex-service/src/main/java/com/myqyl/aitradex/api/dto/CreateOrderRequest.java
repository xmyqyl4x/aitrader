package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.OrderSide;
import com.myqyl.aitradex.domain.OrderSource;
import com.myqyl.aitradex.domain.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID accountId,
    @NotBlank String symbol,
    @NotNull OrderSide side,
    @NotNull OrderType type,
    @NotNull OrderSource source,
    BigDecimal limitPrice,
    BigDecimal stopPrice,
    @NotNull BigDecimal quantity,
    String notes) {}
