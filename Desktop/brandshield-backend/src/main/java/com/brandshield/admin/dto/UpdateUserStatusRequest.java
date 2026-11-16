package com.brandshield.admin.dto;

import com.brandshield.user.entity.Role;

public record UpdateUserStatusRequest(
    Boolean enabled,
    Role role
) {}
