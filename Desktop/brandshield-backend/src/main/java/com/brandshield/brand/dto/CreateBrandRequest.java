package com.brandshield.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateBrandRequest(

    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    String name,

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    String industry,

    @NotBlank(message = "Official website is required")
    @Size(max = 255, message = "Official website must not exceed 255 characters")
    String officialWebsite,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    String logoUrl,

    Set<String> keywords,

    Set<String> officialDomains

) {}
