package com.brandshield.threat.dto;

import com.brandshield.threat.entity.ThreatStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateThreatStatusRequest(

    @NotNull(message = "Threat status is required")
    ThreatStatus status

) {}
