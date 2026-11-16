package com.brandshield.auth.dto;

import com.brandshield.user.entity.Role;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String fullName,
    String email,
    Role role
) {}
