package com.brandshield.threat.service;

import com.brandshield.alert.service.AlertService;
import com.brandshield.brand.entity.Brand;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.exception.ResourceNotFoundException;
import com.brandshield.intelligence.CertificateTransparencyService;
import com.brandshield.intelligence.DnsVerificationService;
import com.brandshield.threat.dto.AnalyzeTargetRequest;
import com.brandshield.threat.dto.ThreatResponse;
import com.brandshield.threat.dto.ThreatStatsResponse;
import com.brandshield.threat.engine.ThreatScoringService;
import com.brandshield.threat.engine.TyposquattingDetector;
import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.Threat;
import com.brandshield.threat.entity.ThreatStatus;
import com.brandshield.threat.repository.ThreatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ThreatDetectionService {

    private final ThreatRepository threatRepository;
    private final BrandRepository brandRepository;
    private final TyposquattingDetector typosquattingDetector;
    private final ThreatScoringService threatScoringService;
    private final AlertService alertService;
    private final CertificateTransparencyService certificateTransparencyService;
    private final DnsVerificationService dnsVerificationService;

    public ThreatDetectionService(
            ThreatRepository threatRepository,
            BrandRepository brandRepository,
            TyposquattingDetector typosquattingDetector,
            ThreatScoringService threatScoringService,
            AlertService alertService,
            CertificateTransparencyService certificateTransparencyService,
            DnsVerificationService dnsVerificationService) {
        this.threatRepository = threatRepository;
        this.brandRepository = brandRepository;
        this.typosquattingDetector = typosquattingDetector;
        this.threatScoringService = threatScoringService;
        this.alertService = alertService;
        this.certificateTransparencyService = certificateTransparencyService;
        this.dnsVerificationService = dnsVerificationService;
    }

    /**
     * Executes an automated scan across brand keywords and lookalike domain variations.
     * Verifies DNS to exclusively discover, track, and alert on active websites.
     */
    @Transactional
    public List<ThreatResponse> scanBrand(UUID userId, UUID brandId) {
        Brand brand = getBrandForUser(userId, brandId);

        Set<String> targetsToEvaluate = new HashSet<>();

        // 1. Generate lookalike, word-reversal, and compound variations for all official domains
        for (String officialDomain : brand.getOfficialDomains()) {
            targetsToEvaluate.addAll(typosquattingDetector.generateVariations(officialDomain, brand.getKeywords()));
        }

        // 2. Discover live registered domains via Certificate Transparency logs
        try {
            var ctEntries = certificateTransparencyService.searchCertificates(brand.getName(), brand.getOfficialDomains());
            for (var entry : ctEntries) {
                targetsToEvaluate.add(entry.domain());
            }
        } catch (Exception ignored) {}

        // 3. Concurrently verify DNS so ONLY active, registered websites on the internet are processed
        Map<String, DnsVerificationService.DnsResult> activeDnsMap = new ConcurrentHashMap<>();
        targetsToEvaluate.parallelStream().forEach(target -> {
            try {
                var dns = dnsVerificationService.verifyDomain(target);
                if (dns.isResolving()) {
                    activeDnsMap.put(target, dns);
                }
            } catch (Exception ignored) {}
        });

        List<Threat> savedThreats = new ArrayList<>();

        for (Map.Entry<String, DnsVerificationService.DnsResult> entry : activeDnsMap.entrySet()) {
            String target = entry.getKey();
            DnsVerificationService.DnsResult dns = entry.getValue();
            ThreatScoringService.AnalysisResult analysis = threatScoringService.analyzeTarget(target, brand);

            // Save threats meeting a medium/high threshold
            if (analysis.score() >= 35) {
                Threat threat = threatRepository.findByBrandId(brandId, Pageable.unpaged())
                        .stream()
                        .filter(t -> t.getTargetValue().equalsIgnoreCase(target))
                        .findFirst()
                        .orElseGet(Threat::new);

                threat.setBrand(brand);
                threat.setTargetValue(target);
                threat.setSource("AUTOMATED_TYPOSQUAT_SCANNER");
                threat.setThreatScore(analysis.score());
                threat.setSeverity(analysis.severity());
                threat.setThreatType(analysis.threatType());
                threat.setStatus(ThreatStatus.DETECTED);
                threat.setLive(true);
                threat.setDetails("Active Host: " + String.join(", ", dns.ipAddresses()) + " | " + analysis.explanation());

                Threat saved = threatRepository.save(threat);
                savedThreats.add(saved);

                // Dispatch automated alert for High/Critical threats
                if (saved.getSeverity() == Severity.CRITICAL || saved.getSeverity() == Severity.HIGH) {
                    alertService.createAlertForThreat(saved);
                }
            }
        }

        return savedThreats.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Analyzes an individual suspected URL or domain on-demand.
     */
    @Transactional
    public ThreatResponse analyzeTarget(UUID userId, AnalyzeTargetRequest request) {
        Brand brand = getBrandForUser(userId, request.brandId());
        String target = request.targetValue().trim();

        ThreatScoringService.AnalysisResult analysis = threatScoringService.analyzeTarget(target, brand);
        DnsVerificationService.DnsResult dnsResult = dnsVerificationService.verifyDomain(target);

        // Find existing or create new threat record
        Threat threat = threatRepository.findByBrandId(brand.getId(), Pageable.unpaged())
                .stream()
                .filter(t -> t.getTargetValue().equalsIgnoreCase(target))
                .findFirst()
                .orElseGet(() -> {
                    Threat t = new Threat();
                    t.setBrand(brand);
                    t.setTargetValue(target);
                    t.setStatus(ThreatStatus.DETECTED);
                    return t;
                });

        threat.setSource("ON_DEMAND_ANALYZER");
        threat.setThreatScore(analysis.score());
        threat.setSeverity(analysis.severity());
        threat.setThreatType(analysis.threatType());
        threat.setLive(dnsResult.isResolving());
        
        String dnsInfo = dnsResult.isResolving()
                ? "Active Host (IPs: " + String.join(", ", dnsResult.ipAddresses()) + ")"
                : "Non-resolving host (NXDOMAIN)";
        threat.setDetails(dnsInfo + " | " + analysis.explanation());

        Threat saved = threatRepository.save(threat);

        // Dispatch automated alert for High/Critical threats
        if (saved.getSeverity() == Severity.CRITICAL || saved.getSeverity() == Severity.HIGH) {
            alertService.createAlertForThreat(saved);
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ThreatResponse> getThreatsByBrand(
            UUID userId,
            UUID brandId,
            ThreatStatus status,
            Severity severity,
            boolean activeOnly,
            Pageable pageable) {

        getBrandForUser(userId, brandId);

        if (activeOnly) {
            if (status != null) {
                return threatRepository.findByBrandIdAndStatusAndIsLive(brandId, status, true, pageable).map(this::mapToResponse);
            } else if (severity != null) {
                return threatRepository.findByBrandIdAndSeverityAndIsLive(brandId, severity, true, pageable).map(this::mapToResponse);
            } else {
                return threatRepository.findByBrandIdAndIsLive(brandId, true, pageable).map(this::mapToResponse);
            }
        } else {
            if (status != null) {
                return threatRepository.findByBrandIdAndStatus(brandId, status, pageable).map(this::mapToResponse);
            } else if (severity != null) {
                return threatRepository.findByBrandIdAndSeverity(brandId, severity, pageable).map(this::mapToResponse);
            } else {
                return threatRepository.findByBrandId(brandId, pageable).map(this::mapToResponse);
            }
        }
    }

    @Transactional(readOnly = true)
    public ThreatResponse getThreatById(UUID userId, UUID threatId) {
        Threat threat = threatRepository.findByIdAndBrandUserId(threatId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Threat not found"));
        return mapToResponse(threat);
    }

    @Transactional
    public ThreatResponse updateThreatStatus(UUID userId, UUID threatId, ThreatStatus newStatus) {
        Threat threat = threatRepository.findByIdAndBrandUserId(threatId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Threat not found"));

        threat.setStatus(newStatus);
        Threat updated = threatRepository.save(threat);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public ThreatStatsResponse getThreatStats(UUID userId, UUID brandId) {
        getBrandForUser(userId, brandId);

        long total = threatRepository.countByBrandId(brandId);
        long critical = threatRepository.countByBrandIdAndSeverity(brandId, Severity.CRITICAL);
        long high = threatRepository.countByBrandIdAndSeverity(brandId, Severity.HIGH);
        long medium = threatRepository.countByBrandIdAndSeverity(brandId, Severity.MEDIUM);
        long low = threatRepository.countByBrandIdAndSeverity(brandId, Severity.LOW);

        long detected = threatRepository.countByBrandIdAndStatus(brandId, ThreatStatus.DETECTED);
        long investigating = threatRepository.countByBrandIdAndStatus(brandId, ThreatStatus.INVESTIGATING);
        long confirmed = threatRepository.countByBrandIdAndStatus(brandId, ThreatStatus.CONFIRMED);
        long resolved = threatRepository.countByBrandIdAndStatus(brandId, ThreatStatus.RESOLVED);
        long falsePositive = threatRepository.countByBrandIdAndStatus(brandId, ThreatStatus.FALSE_POSITIVE);

        Double avgScore = threatRepository.getAverageThreatScoreByBrandId(brandId);

        return new ThreatStatsResponse(
                total,
                critical,
                high,
                medium,
                low,
                detected,
                investigating,
                confirmed,
                resolved,
                falsePositive,
                avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0
        );
    }

    /**
     * Purges or removes non-resolving inactive threats for a brand.
     */
    @Transactional
    public void purgeInactiveThreats(UUID userId, UUID brandId) {
        getBrandForUser(userId, brandId);
        List<Threat> list = threatRepository.findByBrandId(brandId, Pageable.unpaged()).getContent();
        for (Threat t : list) {
            if (!t.isLive()) {
                threatRepository.delete(t);
            }
        }
    }

    private Brand getBrandForUser(UUID userId, UUID brandId) {
        return brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found for this user"));
    }

    private ThreatResponse mapToResponse(Threat threat) {
        return new ThreatResponse(
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
        );
    }
}
