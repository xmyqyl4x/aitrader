package com.myqyl.aitradex.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "quote_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
public class QuoteSnapshot {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(nullable = false, length = 32)
  private String symbol;

  @Column(name = "as_of", nullable = false)
  private OffsetDateTime asOf;

  @Column(precision = 19, scale = 8)
  private BigDecimal open;

  @Column(precision = 19, scale = 8)
  private BigDecimal high;

  @Column(precision = 19, scale = 8)
  private BigDecimal low;

  @Column(precision = 19, scale = 8)
  private BigDecimal close;

  private Long volume;

  @Column(length = 64)
  private String source;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
