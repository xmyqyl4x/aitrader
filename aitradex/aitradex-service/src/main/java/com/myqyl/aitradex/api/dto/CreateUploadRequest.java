package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.UploadType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateUploadRequest(
    @NotNull UUID userId, @NotNull UploadType type, @NotBlank String fileName, String storedPath) {}
