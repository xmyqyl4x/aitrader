package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.StockQuoteSearch;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockQuoteSearchDto(
    UUID id,
    OffsetDateTime createdAt,
    UUID createdByUserId,
    String symbol,
    String companyName,
    String exchange,
    String range,
    OffsetDateTime requestedAt,
    String status,
    OffsetDateTime quoteTimestamp,
    BigDecimal price,
    String currency,
    BigDecimal changeAmount,
    BigDecimal changePercent,
    Long volume,
    String provider,
    String requestId,
    String correlationId,
    String errorCode,
    String errorMessage,
    Integer durationMs,
    String reviewStatus,
    String reviewNote,
    OffsetDateTime reviewedAt) {

  public static StockQuoteSearchDto fromEntity(StockQuoteSearch entity) {
    return new StockQuoteSearchDto(
        entity.getId(),
        entity.getCreatedAt(),
        entity.getCreatedByUserId(),
        entity.getSymbol(),
        entity.getCompanyName(),
        entity.getExchange(),
        entity.getRange() != null ? entity.getRange().getValue() : null,
        entity.getRequestedAt(),
        entity.getStatus() != null ? entity.getStatus().name() : null,
        entity.getQuoteTimestamp(),
        entity.getPrice(),
        entity.getCurrency(),
        entity.getChangeAmount(),
        entity.getChangePercent(),
        entity.getVolume(),
        entity.getProvider(),
        entity.getRequestId(),
        entity.getCorrelationId(),
        entity.getErrorCode(),
        entity.getErrorMessage(),
        entity.getDurationMs(),
        entity.getReviewStatus() != null ? entity.getReviewStatus().name() : null,
        entity.getReviewNote(),
        entity.getReviewedAt());
  }
}
