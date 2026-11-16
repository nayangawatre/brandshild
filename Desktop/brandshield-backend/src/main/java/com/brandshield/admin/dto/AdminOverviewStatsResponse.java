package com.brandshield.admin.dto;

public record AdminOverviewStatsResponse(
    long totalUsers,
    long activeUsers,
    long totalBrands,
    long activeBrands,
    long totalThreats,
    long criticalThreats,
    long resolvedThreats,
    long totalAlerts,
    double resolutionRate
) {}
