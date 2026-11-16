package com.brandshield.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String action,
    String entityType,
    String entityId,
    String details,
    String ipAddress,
    LocalDateTime createdAt
) {}
