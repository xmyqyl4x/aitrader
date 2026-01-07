package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.UploadStatus;
import com.myqyl.aitradex.domain.UploadType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadDto(
    UUID id,
    UUID userId,
    UploadType type,
    UploadStatus status,
    String fileName,
    String storedPath,
    Integer parsedRowCount,
    String errorReport,
    OffsetDateTime createdAt,
    OffsetDateTime completedAt) {}
