package com.myqyl.aitradex.api.dto;

import java.time.OffsetDateTime;

public record UpdatePositionCloseRequest(OffsetDateTime closedAt) {}
