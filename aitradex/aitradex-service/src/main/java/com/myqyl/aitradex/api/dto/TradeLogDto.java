package com.myqyl.aitradex.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TradeLogDto(
    UUID id,
    UUID accountId,
    String action,
    String reason,
    String metadata,
    OffsetDateTime occurredAt) {}
