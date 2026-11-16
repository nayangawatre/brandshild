package com.brandshield.admin.controller;

import com.brandshield.admin.dto.AdminOverviewStatsResponse;
import com.brandshield.admin.dto.AdminUserSummaryResponse;
import com.brandshield.admin.dto.AuditLogResponse;
import com.brandshield.admin.dto.UpdateUserStatusRequest;
import com.brandshield.admin.service.AdminService;
import com.brandshield.admin.service.AuditLogService;
import com.brandshield.brand.dto.BrandResponse;
import com.brandshield.security.CustomUserDetails;
import com.brandshield.threat.dto.ThreatResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    public AdminController(AdminService adminService, AuditLogService auditLogService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/stats")
    public AdminOverviewStatsResponse getPlatformStats() {
        return adminService.getPlatformStats();
    }

    @GetMapping("/users")
    public Page<AdminUserSummaryResponse> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return adminService.getAllUsers(pageable);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserSummaryResponse updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable UUID userId,
            @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        return adminService.updateUserStatus(adminDetails.getUser().getId(), userId, request, ipAddress);
    }

    @GetMapping("/brands")
    public Page<BrandResponse> getAllBrands(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return adminService.getAllBrands(pageable);
    }

    @GetMapping("/threats")
    public Page<ThreatResponse> getAllThreats(
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return adminService.getAllThreats(pageable);
    }

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return auditLogService.getAuditLogs(pageable);
    }
}
