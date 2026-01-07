package com.myqyl.aitradex.api.dto;

import java.time.Instant;

/**
 * Represents an active quote streaming subscription.
 */
public record QuoteStreamSubscription(
    String subscriptionId,
    String symbol,
    String source,
    Instant startedAt,
    Instant expiresAt,
    long pollIntervalMs) {}
