package com.brandshield.brand.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AddKeywordsRequest(

    @NotEmpty(message = "At least one keyword is required")
    Set<String> keywords

) {}
