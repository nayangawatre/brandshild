package com.brandshield.threat.controller;

import com.brandshield.security.CustomUserDetails;
import com.brandshield.threat.dto.AnalyzeTargetRequest;
import com.brandshield.threat.dto.ThreatResponse;
import com.brandshield.threat.dto.ThreatStatsResponse;
import com.brandshield.threat.dto.UpdateThreatStatusRequest;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.ThreatStatus;
import com.brandshield.threat.service.ThreatDetectionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/threats")
public class ThreatController {

    private final ThreatDetectionService threatDetectionService;

    public ThreatController(ThreatDetectionService threatDetectionService) {
        this.threatDetectionService = threatDetectionService;
    }

    @PostMapping("/scan/{brandId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<ThreatResponse> triggerScan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId) {

        return threatDetectionService.scanBrand(userDetails.getUser().getId(), brandId);
    }

    @PostMapping("/analyze")
    public ThreatResponse analyzeTarget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AnalyzeTargetRequest request) {

        return threatDetectionService.analyzeTarget(userDetails.getUser().getId(), request);
    }

    @GetMapping("/brand/{brandId}")
    public Page<ThreatResponse> getThreatsByBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId,
            @RequestParam(required = false) ThreatStatus status,
            @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 20, sort = "threatScore", direction = Sort.Direction.DESC) Pageable pageable) {

        return threatDetectionService.getThreatsByBrand(
                userDetails.getUser().getId(), brandId, status, severity, pageable
        );
    }

    @GetMapping("/{id}")
    public ThreatResponse getThreatById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        return threatDetectionService.getThreatById(userDetails.getUser().getId(), id);
    }

    @PatchMapping("/{id}/status")
    public ThreatResponse updateThreatStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThreatStatusRequest request) {

        return threatDetectionService.updateThreatStatus(
                userDetails.getUser().getId(), id, request.status()
        );
    }

    @GetMapping("/stats/{brandId}")
    public ThreatStatsResponse getThreatStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId) {

        return threatDetectionService.getThreatStats(userDetails.getUser().getId(), brandId);
    }
}
