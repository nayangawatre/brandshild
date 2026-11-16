package com.brandshield.alert.controller;

import com.brandshield.alert.dto.AlertResponse;
import com.brandshield.alert.dto.UnreadCountResponse;
import com.brandshield.alert.entity.AlertStatus;
import com.brandshield.alert.service.AlertService;
import com.brandshield.security.CustomUserDetails;
import com.brandshield.threat.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public Page<AlertResponse> getUserAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return alertService.getUserAlerts(userDetails.getUser().getId(), status, severity, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return alertService.getUnreadCount(userDetails.getUser().getId());
    }

    @PatchMapping("/{id}/read")
    public AlertResponse markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        return alertService.markAsRead(userDetails.getUser().getId(), id);
    }

    @PatchMapping("/read-all")
    public Map<String, Object> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        int updated = alertService.markAllAsRead(userDetails.getUser().getId());
        return Map.of("updatedCount", updated, "message", "All unread alerts marked as read");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        alertService.dismissAlert(userDetails.getUser().getId(), id);
    }
}
