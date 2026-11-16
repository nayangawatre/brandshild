package com.brandshield.alert.repository;

import com.brandshield.alert.entity.Alert;
import com.brandshield.alert.entity.AlertStatus;
import com.brandshield.threat.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByUserId(UUID userId, Pageable pageable);

    Page<Alert> findByUserIdAndStatus(UUID userId, AlertStatus status, Pageable pageable);

    Page<Alert> findByUserIdAndSeverity(UUID userId, Severity severity, Pageable pageable);

    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatus(UUID userId, AlertStatus status);

    boolean existsByThreatId(UUID threatId);

    @Modifying
    @Query("UPDATE Alert a SET a.status = :status, a.readAt = :now WHERE a.user.id = :userId AND a.status = 'UNREAD'")
    int markAllAsRead(
            @Param("userId") UUID userId,
            @Param("status") AlertStatus status,
            @Param("now") LocalDateTime now
    );
}
