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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserRole role;

  @Column(name = "api_key_hash")
  private String apiKeyHash;

  @Column(name = "oidc_subject")
  private String oidcSubject;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
