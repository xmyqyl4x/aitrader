package com.myqyl.aitradex.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBenchmarkRequest(@NotBlank String symbol, String name) {}
