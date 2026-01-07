package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
    UUID id, String email, String displayName, UserRole role, OffsetDateTime createdAt) {}
