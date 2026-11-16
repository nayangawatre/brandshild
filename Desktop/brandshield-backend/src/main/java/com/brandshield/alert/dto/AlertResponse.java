package com.brandshield.alert.dto;

import com.brandshield.alert.entity.AlertChannel;
import com.brandshield.alert.entity.AlertStatus;
import com.brandshield.threat.entity.Severity;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertResponse(
    UUID id,
    UUID brandId,
    String brandName,
    UUID threatId,
    String title,
    String message,
    Severity severity,
    AlertChannel channel,
    AlertStatus status,
    LocalDateTime sentAt,
    LocalDateTime readAt
) {}
