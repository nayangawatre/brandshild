package com.brandshield.brand.repository;

import com.brandshield.brand.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Page<Brand> findByUserId(UUID userId, Pageable pageable);

    List<Brand> findByUserId(UUID userId);

    Optional<Brand> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, UUID userId);

    long countByUserId(UUID userId);
}
