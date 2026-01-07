package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
    @NotNull UUID userId, @NotBlank String baseCurrency, @NotNull BigDecimal initialCash) {}
