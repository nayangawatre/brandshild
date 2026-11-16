package com.brandshield.threat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AnalyzeTargetRequest(

    @NotNull(message = "Brand ID is required")
    UUID brandId,

    @NotBlank(message = "Target URL or domain is required")
    @Size(max = 500, message = "Target must not exceed 500 characters")
    String targetValue

) {}
