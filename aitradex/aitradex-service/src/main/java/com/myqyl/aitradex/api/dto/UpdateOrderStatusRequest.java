package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record UpdateOrderStatusRequest(
    @NotNull OrderStatus status, OffsetDateTime routedAt, OffsetDateTime filledAt, String notes) {}
