package com.brandshield.admin.service;

import com.brandshield.admin.dto.AdminOverviewStatsResponse;
import com.brandshield.admin.dto.AdminUserSummaryResponse;
import com.brandshield.admin.dto.UpdateUserStatusRequest;
import com.brandshield.alert.repository.AlertRepository;
import com.brandshield.brand.dto.BrandResponse;
import com.brandshield.brand.entity.Brand;
import com.brandshield.brand.entity.BrandStatus;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.exception.ResourceNotFoundException;
import com.brandshield.threat.dto.ThreatResponse;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.Threat;
import com.brandshield.threat.entity.ThreatStatus;
import com.brandshield.threat.repository.ThreatRepository;
import com.brandshield.user.entity.User;
import com.brandshield.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final ThreatRepository threatRepository;
    private final AlertRepository alertRepository;
    private final AuditLogService auditLogService;

    public AdminService(
            UserRepository userRepository,
            BrandRepository brandRepository,
            ThreatRepository threatRepository,
            AlertRepository alertRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.threatRepository = threatRepository;
        this.alertRepository = alertRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminOverviewStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isEnabled).count();

        long totalBrands = brandRepository.count();
        long activeBrands = brandRepository.findAll().stream()
                .filter(b -> b.getStatus() == BrandStatus.ACTIVE)
                .count();

        long totalThreats = threatRepository.count();
        long criticalThreats = threatRepository.findAll().stream()
                .filter(t -> t.getSeverity() == Severity.CRITICAL)
                .count();
        long resolvedThreats = threatRepository.findAll().stream()
                .filter(t -> t.getStatus() == ThreatStatus.RESOLVED)
                .count();

        long totalAlerts = alertRepository.count();

        double resolutionRate = totalThreats > 0
                ? Math.round(((double) resolvedThreats / totalThreats * 100.0) * 10.0) / 10.0
                : 0.0;

        return new AdminOverviewStatsResponse(
                totalUsers,
                activeUsers,
                totalBrands,
                activeBrands,
                totalThreats,
                criticalThreats,
                resolvedThreats,
                totalAlerts,
                resolutionRate
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> new AdminUserSummaryResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        user.isEnabled(),
                        brandRepository.countByUserId(user.getId()),
                        user.getCreatedAt()
                ));
    }

    @Transactional
    public AdminUserSummaryResponse updateUserStatus(
            UUID adminUserId,
            UUID targetUserId,
            UpdateUserStatusRequest request,
            String ipAddress) {

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        User updated = userRepository.save(user);

        // Record audit trail
        auditLogService.logAction(
                adminUserId,
                "UPDATE_USER_STATUS",
                "USER",
                targetUserId.toString(),
                String.format("Updated user status: enabled=%s, role=%s", updated.isEnabled(), updated.getRole()),
                ipAddress
        );

        return new AdminUserSummaryResponse(
                updated.getId(),
                updated.getFullName(),
                updated.getEmail(),
                updated.getRole(),
                updated.isEnabled(),
                brandRepository.countByUserId(updated.getId()),
                updated.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<BrandResponse> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(brand -> new BrandResponse(
                        brand.getId(),
                        brand.getUser().getId(),
                        brand.getName(),
                        brand.getIndustry(),
                        brand.getOfficialWebsite(),
                        brand.getDescription(),
                        brand.getLogoUrl(),
                        brand.getStatus(),
                        brand.getKeywords(),
                        brand.getOfficialDomains(),
                        brand.getCreatedAt(),
                        brand.getUpdatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<ThreatResponse> getAllThreats(Pageable pageable) {
        return threatRepository.findAll(pageable)
                .map(threat -> new ThreatResponse(
                        threat.getId(),
                        threat.getBrand().getId(),
                        threat.getBrand().getName(),
                        threat.getThreatType(),
                        threat.getSeverity(),
                        threat.getStatus(),
                        threat.getTargetValue(),
                        threat.getSource(),
                        threat.getThreatScore(),
                        threat.getDetails(),
                        threat.isLive(),
                        threat.getDetectedAt(),
                        threat.getUpdatedAt()
                ));
    }
}
