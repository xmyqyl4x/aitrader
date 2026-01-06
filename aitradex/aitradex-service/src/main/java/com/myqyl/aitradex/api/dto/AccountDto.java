package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDto(
    UUID id, UUID userId, String baseCurrency, BigDecimal cashBalance, OffsetDateTime createdAt) {}
