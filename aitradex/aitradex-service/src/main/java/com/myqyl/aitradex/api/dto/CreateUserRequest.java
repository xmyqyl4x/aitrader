package com.myqyl.aitradex.api.dto;

import com.myqyl.aitradex.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @Email @NotBlank String email, @NotBlank String displayName, @NotNull UserRole role) {}
