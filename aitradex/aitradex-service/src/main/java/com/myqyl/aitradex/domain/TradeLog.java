package com.myqyl.aitradex.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "trade_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(length = 512)
  private String reason;

  @Column(columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "occurred_at", nullable = false)
  private OffsetDateTime occurredAt;
}
