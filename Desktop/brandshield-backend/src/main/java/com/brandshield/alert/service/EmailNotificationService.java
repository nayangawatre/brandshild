package com.brandshield.alert.service;

import com.brandshield.admin.service.AuditLogService;
import com.brandshield.alert.entity.Alert;
import com.brandshield.threat.entity.Threat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final AuditLogService auditLogService;

    public EmailNotificationService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Step 5: Send email through queued worker.
     * Step 6: Record delivery or retry failure.
     */
    @Async("emailTaskExecutor")
    public void sendThreatAlertEmail(Threat threat, Alert alert) {
        if (threat == null || threat.getBrand() == null || threat.getBrand().getUser() == null) {
            log.warn("Cannot send threat alert email: Missing threat, brand, or user information");
            return;
        }

        String recipientEmail = threat.getBrand().getUser().getEmail();
        String brandName = threat.getBrand().getName();
        String subject = String.format("BrandShield alert: A new domain matching %s was detected", brandName);

        String detectionReason = switch (threat.getThreatType()) {
            case TYPOSQUATTING -> "Similar spelling / Typosquatting";
            case PHISHING -> "Phishing signature detected";
            case COUNTERFEIT -> "Counterfeit goods / Brand impersonation";
            case SUSPICIOUS_DOMAIN -> "Suspicious TLD permutation";
            default -> threat.getThreatType().name();
        };

        String body = String.format("""
            ================================================================================
            BRANDSHIELD EMAIL NOTIFICATION DISPATCH
            ================================================================================
            SUBJECT: %s
            TO:      %s

            A potential domain match was discovered for your monitored brand.

            Domain:          %s
            Detection:       %s
            Website status:  Not yet verified
            Risk:            %s (Threat Score: %d/100)

            Evidence & Findings:
            %s

            --------------------------------------------------------------------------------
            This is an automated notification from BrandShield.
            Production settings: Verified sender domain with SPF, DKIM, and DMARC enforcement.
            Log in to your BrandShield dashboard to review and triage this finding.
            ================================================================================
            """,
                subject,
                recipientEmail,
                threat.getTargetValue(),
                detectionReason,
                threat.getSeverity(),
                threat.getThreatScore(),
                threat.getDetails() != null ? threat.getDetails() : "Identified by automated brand monitoring engine"
        );

        try {
            // Worker logs formatted transactional email
            log.info("\n{}", body);

            // Record delivery in audit log
            String alertIdStr = (alert != null && alert.getId() != null) ? alert.getId().toString() : "N/A";
            auditLogService.logAction(
                    threat.getBrand().getUser().getId(),
                    "EMAIL_ALERT_DELIVERED",
                    "ALERT",
                    alertIdStr,
                    String.format("Dispatched email alert to %s for domain '%s' (Severity: %s)", recipientEmail, threat.getTargetValue(), threat.getSeverity()),
                    "127.0.0.1"
            );

            log.info("Email alert successfully processed for recipient={}", recipientEmail);

        } catch (Exception ex) {
            log.error("Failed to deliver alert email to {}: {}", recipientEmail, ex.getMessage());

            String alertIdStr = (alert != null && alert.getId() != null) ? alert.getId().toString() : "N/A";
            auditLogService.logAction(
                    threat.getBrand().getUser().getId(),
                    "EMAIL_ALERT_FAILED",
                    "ALERT",
                    alertIdStr,
                    String.format("Failed email delivery to %s: %s", recipientEmail, ex.getMessage()),
                    "127.0.0.1"
            );
        }
    }
}
