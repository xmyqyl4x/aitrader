package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTradeLogRequest(
    @NotNull UUID accountId, @NotBlank String action, String reason, String metadata) {}
