package com.brandshield.admin.dto;

import com.brandshield.user.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserSummaryResponse(
    UUID id,
    String fullName,
    String email,
    Role role,
    boolean enabled,
    long brandCount,
    LocalDateTime createdAt
) {}
