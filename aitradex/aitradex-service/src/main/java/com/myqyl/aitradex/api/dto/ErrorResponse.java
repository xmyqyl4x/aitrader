package com.myqyl.aitradex.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    String error,
    String message,
    int status,
    OffsetDateTime timestamp,
    List<String> details) {}
