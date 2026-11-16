package com.brandshield.alert.scheduler;

import com.brandshield.brand.entity.Brand;
import com.brandshield.brand.entity.BrandStatus;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.threat.service.ThreatDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.monitoring.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BrandMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(BrandMonitoringScheduler.class);

    private final BrandRepository brandRepository;
    private final ThreatDetectionService threatDetectionService;

    public BrandMonitoringScheduler(
            BrandRepository brandRepository,
            ThreatDetectionService threatDetectionService) {
        this.brandRepository = brandRepository;
        this.threatDetectionService = threatDetectionService;
    }

    /**
     * Periodic background scan job across all active brands.
     * Defaults to running every 6 hours, or configured via app.monitoring.cron.
     */
    @Scheduled(cron = "${app.monitoring.cron:0 0 */6 * * *}")
    public void runScheduledMonitoring() {
        log.info("Starting automated scheduled monitoring cycle...");

        List<Brand> activeBrands = brandRepository.findAll().stream()
                .filter(b -> b.getStatus() == BrandStatus.ACTIVE)
                .toList();

        log.info("Found {} active brands for scanning", activeBrands.size());

        for (Brand brand : activeBrands) {
            try {
                var threats = threatDetectionService.scanBrand(brand.getUser().getId(), brand.getId());
                log.info("Scanned brand '{}' (ID={}): {} new threats flagged",
                        brand.getName(), brand.getId(), threats.size());
            } catch (Exception e) {
                log.error("Error during scheduled scan for brand ID={}: {}", brand.getId(), e.getMessage());
            }
        }

        log.info("Completed automated scheduled monitoring cycle.");
    }
}
