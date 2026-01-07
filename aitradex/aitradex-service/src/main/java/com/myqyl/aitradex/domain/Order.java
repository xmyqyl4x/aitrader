package com.myqyl.aitradex.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
public class Order {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 32)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private OrderSide side;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private OrderType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private OrderStatus status;

  @Column(name = "limit_price", precision = 19, scale = 8)
  private BigDecimal limitPrice;

  @Column(name = "stop_price", precision = 19, scale = 8)
  private BigDecimal stopPrice;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal quantity;

  @Column(name = "routed_at")
  private OffsetDateTime routedAt;

  @Column(name = "filled_at")
  private OffsetDateTime filledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private OrderSource source;

  @Column(length = 512)
  private String notes;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
