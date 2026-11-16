package com.brandshield.threat.dto;

public record ThreatStatsResponse(
    long totalThreats,
    long criticalCount,
    long highCount,
    long mediumCount,
    long lowCount,
    long detectedCount,
    long investigatingCount,
    long confirmedCount,
    long resolvedCount,
    long falsePositiveCount,
    double averageThreatScore
) {}
