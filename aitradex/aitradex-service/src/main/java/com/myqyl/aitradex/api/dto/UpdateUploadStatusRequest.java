package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.UploadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUploadStatusRequest(
    @NotNull UploadStatus status, Integer parsedRowCount, String errorReport, String storedPath) {}
