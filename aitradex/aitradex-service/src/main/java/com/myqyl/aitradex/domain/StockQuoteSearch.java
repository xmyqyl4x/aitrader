package com.myqyl.aitradex.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "stock_quote_search")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
public class StockQuoteSearch {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private OffsetDateTime createdAt;

  @Column(name = "created_by_user_id")
  private UUID createdByUserId;

  // Search request data
  @Column(nullable = false, length = 32)
  private String symbol;

  @Column(name = "company_name", length = 255)
  private String companyName;

  @Column(length = 32)
  private String exchange;

  @Column(nullable = false, length = 10)
  @Enumerated(EnumType.STRING)
  private QuoteRange range;

  @Column(name = "requested_at", nullable = false)
  private OffsetDateTime requestedAt;

  // Result metadata
  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SearchStatus status;

  @Column(name = "quote_timestamp")
  private OffsetDateTime quoteTimestamp;

  @Column(precision = 19, scale = 8)
  private BigDecimal price;

  @Column(length = 10)
  private String currency;

  @Column(name = "change_amount", precision = 19, scale = 8)
  private BigDecimal changeAmount;

  @Column(name = "change_percent", precision = 19, scale = 8)
  private BigDecimal changePercent;

  private Long volume;

  // Operational / troubleshooting
  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "request_id", length = 255)
  private String requestId;

  @Column(name = "correlation_id", length = 255)
  private String correlationId;

  @Column(name = "error_code", length = 50)
  private String errorCode;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "duration_ms")
  private Integer durationMs;

  // Review fields
  @Column(name = "review_status", length = 20)
  @Enumerated(EnumType.STRING)
  private ReviewStatus reviewStatus;

  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  public enum QuoteRange {
    ONE_DAY("1D"),
    FIVE_DAYS("5D"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    ONE_YEAR("1Y");

    private final String value;

    QuoteRange(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static QuoteRange fromValue(String value) {
      for (QuoteRange range : values()) {
        if (range.value.equals(value)) {
          return range;
        }
      }
      throw new IllegalArgumentException("Unknown range: " + value);
    }
  }

  public enum SearchStatus {
    SUCCESS,
    FAILED
  }

  public enum ReviewStatus {
    NOT_REVIEWED,
    REVIEWED
  }
}
