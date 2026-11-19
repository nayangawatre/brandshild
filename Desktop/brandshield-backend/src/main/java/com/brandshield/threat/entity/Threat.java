package com.brandshield.threat.entity;

import com.brandshield.brand.entity.Brand;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "threats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_threat_brand_target", columnNames = {"brand_id", "target_value"})
})
public class Threat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 30)
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ThreatStatus status = ThreatStatus.DETECTED;

    @Column(name = "target_value", nullable = false, length = 500)
    private String targetValue;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "threat_score", nullable = false)
    private int threatScore;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "is_live", nullable = false)
    private boolean isLive = true;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.detectedAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ThreatStatus.DETECTED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Threat() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public ThreatType getThreatType() {
        return threatType;
    }

    public void setThreatType(ThreatType threatType) {
        this.threatType = threatType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public ThreatStatus getStatus() {
        return status;
    }

    public void setStatus(ThreatStatus status) {
        this.status = status;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(int threatScore) {
        this.threatScore = threatScore;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public void setIsLive(boolean isLive) {
        this.isLive = isLive;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
