package com.brandshield.brand.dto;

import com.brandshield.brand.entity.BrandStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record BrandResponse(
    UUID id,
    UUID userId,
    String name,
    String industry,
    String officialWebsite,
    String description,
    String logoUrl,
    BrandStatus status,
    Set<String> keywords,
    Set<String> officialDomains,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
