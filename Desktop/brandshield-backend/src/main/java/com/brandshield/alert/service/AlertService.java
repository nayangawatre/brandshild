package com.brandshield.alert.service;

import com.brandshield.alert.dto.AlertResponse;
import com.brandshield.alert.dto.UnreadCountResponse;
import com.brandshield.alert.entity.Alert;
import com.brandshield.alert.entity.AlertChannel;
import com.brandshield.alert.entity.AlertStatus;
import com.brandshield.alert.repository.AlertRepository;
import com.brandshield.exception.ResourceNotFoundException;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.Threat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert createAlertForThreat(Threat threat) {
        if (threat.getId() != null && alertRepository.existsByThreatId(threat.getId())) {
            return null; // Prevent duplicate alert for the same threat
        }

        String brandName = threat.getBrand().getName();
        String title = String.format("[%s] %s Detected for %s",
                threat.getSeverity(),
                threat.getThreatType().name().replace("_", " "),
                brandName
        );

        String message = String.format(
                "A %s severity threat was identified targeting '%s' (Threat Score: %d/100). Target: %s. Details: %s",
                threat.getSeverity(),
                brandName,
                threat.getThreatScore(),
                threat.getTargetValue(),
                threat.getDetails()
        );

        Alert alert = new Alert();
        alert.setUser(threat.getBrand().getUser());
        alert.setBrand(threat.getBrand());
        alert.setThreat(threat);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setSeverity(threat.getSeverity());
        alert.setChannel(AlertChannel.IN_APP);
        alert.setStatus(AlertStatus.UNREAD);

        Alert saved = alertRepository.save(alert);
        log.info("Dispatched {} alert ID={} for user={}", threat.getSeverity(), saved.getId(), threat.getBrand().getUser().getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> getUserAlerts(
            UUID userId,
            AlertStatus status,
            Severity severity,
            Pageable pageable) {

        if (status != null) {
            return alertRepository.findByUserIdAndStatus(userId, status, pageable).map(this::mapToResponse);
        } else if (severity != null) {
            return alertRepository.findByUserIdAndSeverity(userId, severity, pageable).map(this::mapToResponse);
        } else {
            return alertRepository.findByUserId(userId, pageable).map(this::mapToResponse);
        }
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = alertRepository.countByUserIdAndStatus(userId, AlertStatus.UNREAD);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public AlertResponse markAsRead(UUID userId, UUID alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        alert.setStatus(AlertStatus.READ);
        alert.setReadAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);
        return mapToResponse(saved);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return alertRepository.markAllAsRead(userId, AlertStatus.READ, LocalDateTime.now());
    }

    @Transactional
    public void dismissAlert(UUID userId, UUID alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        alert.setStatus(AlertStatus.DISMISSED);
        alertRepository.save(alert);
    }

    private AlertResponse mapToResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getBrand().getId(),
                alert.getBrand().getName(),
                alert.getThreat() != null ? alert.getThreat().getId() : null,
                alert.getTitle(),
                alert.getMessage(),
                alert.getSeverity(),
                alert.getChannel(),
                alert.getStatus(),
                alert.getSentAt(),
                alert.getReadAt()
        );
    }
}
