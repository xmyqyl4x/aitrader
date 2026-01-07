package com.myqyl.aitradex.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
public class AuditLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(nullable = false, length = 64)
  private String actor;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_type", nullable = false, length = 32)
  private ActorType actorType;

  @Column(nullable = false, length = 128)
  private String action;

  @Column(name = "entity_ref", nullable = false, length = 128)
  private String entityRef;

  @Column(name = "before_state", columnDefinition = "jsonb")
  private String beforeState;

  @Column(name = "after_state", columnDefinition = "jsonb")
  private String afterState;

  @CreatedDate
  @Column(name = "occurred_at", nullable = false, updatable = false)
  private OffsetDateTime occurredAt;
}
