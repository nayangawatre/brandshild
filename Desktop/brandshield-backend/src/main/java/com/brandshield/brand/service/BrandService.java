package com.brandshield.brand.service;

import com.brandshield.brand.dto.AddKeywordsRequest;
import com.brandshield.brand.dto.BrandResponse;
import com.brandshield.brand.dto.CreateBrandRequest;
import com.brandshield.brand.dto.UpdateBrandRequest;
import com.brandshield.brand.entity.Brand;
import com.brandshield.brand.entity.BrandStatus;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.exception.ResourceNotFoundException;
import com.brandshield.user.entity.User;
import com.brandshield.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BrandResponse createBrand(UUID userId, CreateBrandRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String trimmedName = request.name().trim();
        if (brandRepository.existsByNameIgnoreCaseAndUserId(trimmedName, userId)) {
            throw new IllegalArgumentException("A brand with the name '" + trimmedName + "' already exists");
        }

        Brand brand = new Brand();
        brand.setUser(user);
        brand.setName(trimmedName);
        brand.setIndustry(request.industry() != null ? request.industry().trim() : null);
        brand.setOfficialWebsite(request.officialWebsite().trim());
        brand.setDescription(request.description() != null ? request.description().trim() : null);
        brand.setLogoUrl(request.logoUrl() != null ? request.logoUrl().trim() : null);
        brand.setStatus(BrandStatus.ACTIVE);

        // Sanitize and populate keywords
        Set<String> keywords = new HashSet<>();
        if (request.keywords() != null) {
            request.keywords().stream()
                    .map(String::trim)
                    .filter(k -> !k.isBlank())
                    .forEach(keywords::add);
        }
        // Always include brand name as a default keyword
        keywords.add(trimmedName.toLowerCase());
        brand.setKeywords(keywords);

        // Sanitize and populate official domains
        Set<String> officialDomains = new HashSet<>();
        if (request.officialDomains() != null) {
            request.officialDomains().stream()
                    .map(this::normalizeDomain)
                    .filter(d -> !d.isBlank())
                    .forEach(officialDomains::add);
        }
        // Automatically extract domain from official website
        String siteDomain = normalizeDomain(request.officialWebsite());
        if (!siteDomain.isBlank()) {
            officialDomains.add(siteDomain);
        }
        brand.setOfficialDomains(officialDomains);

        Brand savedBrand = brandRepository.save(brand);
        return mapToResponse(savedBrand);
    }

    @Transactional(readOnly = true)
    public Page<BrandResponse> getUserBrands(UUID userId, Pageable pageable) {
        return brandRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID userId, UUID brandId) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        return mapToResponse(brand);
    }

    @Transactional
    public BrandResponse updateBrand(UUID userId, UUID brandId, UpdateBrandRequest request) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        String newName = request.name().trim();
        if (!brand.getName().equalsIgnoreCase(newName) &&
                brandRepository.existsByNameIgnoreCaseAndUserId(newName, userId)) {
            throw new IllegalArgumentException("A brand with the name '" + newName + "' already exists");
        }

        brand.setName(newName);
        brand.setIndustry(request.industry() != null ? request.industry().trim() : null);
        brand.setOfficialWebsite(request.officialWebsite().trim());
        brand.setDescription(request.description() != null ? request.description().trim() : null);
        brand.setLogoUrl(request.logoUrl() != null ? request.logoUrl().trim() : null);
        if (request.status() != null) {
            brand.setStatus(request.status());
        }

        // Ensure current site domain is in officialDomains
        String siteDomain = normalizeDomain(request.officialWebsite());
        if (!siteDomain.isBlank()) {
            brand.getOfficialDomains().add(siteDomain);
        }

        Brand updated = brandRepository.save(brand);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteBrand(UUID userId, UUID brandId) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        brandRepository.delete(brand);
    }

    @Transactional
    public BrandResponse addKeywords(UUID userId, UUID brandId, AddKeywordsRequest request) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        Set<String> newKeywords = request.keywords().stream()
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .collect(Collectors.toSet());

        brand.getKeywords().addAll(newKeywords);
        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    @Transactional
    public BrandResponse removeKeyword(UUID userId, UUID brandId, String keyword) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        brand.getKeywords().remove(keyword.trim().toLowerCase());
        brand.getKeywords().remove(keyword.trim());

        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    private String normalizeDomain(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String clean = input.trim().toLowerCase();
        try {
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                clean = "https://" + clean;
            }
            URI uri = URI.create(clean);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        // Fallback cleanup
        return clean.replaceFirst("^(https?://)?(www\\.)?", "")
                .split("/")[0]
                .trim();
    }

    private BrandResponse mapToResponse(Brand brand) {
        return new BrandResponse(
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
        );
    }
}
