package com.brandshield.threat.repository;

import com.brandshield.threat.entity.Severity;
import com.brandshield.threat.entity.Threat;
import com.brandshield.threat.entity.ThreatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreatRepository extends JpaRepository<Threat, UUID> {

    Page<Threat> findByBrandId(UUID brandId, Pageable pageable);

    Page<Threat> findByBrandIdAndStatus(UUID brandId, ThreatStatus status, Pageable pageable);

    Page<Threat> findByBrandIdAndSeverity(UUID brandId, Severity severity, Pageable pageable);

    Optional<Threat> findByIdAndBrandUserId(UUID id, UUID userId);

    boolean existsByBrandIdAndTargetValue(UUID brandId, String targetValue);

    long countByBrandId(UUID brandId);

    long countByBrandIdAndSeverity(UUID brandId, Severity severity);

    long countByBrandIdAndStatus(UUID brandId, ThreatStatus status);

    @Query("SELECT COALESCE(AVG(t.threatScore), 0.0) FROM Threat t WHERE t.brand.id = :brandId")
    Double getAverageThreatScoreByBrandId(@Param("brandId") UUID brandId);
}
