package com.brandshield.threat.dto;

import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.ThreatStatus;
import com.brandshield.threat.entity.ThreatType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThreatResponse(
    UUID id,
    UUID brandId,
    String brandName,
    ThreatType threatType,
    Severity severity,
    ThreatStatus status,
    String targetValue,
    String source,
    int threatScore,
    String details,
    boolean isLive,
    LocalDateTime detectedAt,
    LocalDateTime updatedAt
) {
    public ThreatResponse(
            UUID id,
            UUID brandId,
            String brandName,
            ThreatType threatType,
            Severity severity,
            ThreatStatus status,
            String targetValue,
            String source,
            int threatScore,
            String details,
            LocalDateTime detectedAt,
            LocalDateTime updatedAt
    ) {
        this(id, brandId, brandName, threatType, severity, status, targetValue, source, threatScore, details, true, detectedAt, updatedAt);
    }

    public int score() {
        return threatScore;
    }

    public ThreatType type() {
        return threatType;
    }
}
