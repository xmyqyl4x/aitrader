package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStockQuoteReviewRequest(@NotBlank String reviewStatus, String reviewNote) {}
