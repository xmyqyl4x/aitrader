package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateStopLossRequest(@NotNull BigDecimal stopLoss) {}
