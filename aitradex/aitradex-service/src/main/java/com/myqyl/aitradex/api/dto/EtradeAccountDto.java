package com.myqyl.aitradex.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EtradeAccountDto(
    UUID id,
    UUID userId,
    String accountIdKey,
    String accountType,
    String accountName,
    String accountStatus,
    OffsetDateTime linkedAt,
    OffsetDateTime lastSyncedAt) {}
