package com.brandshield.brand.controller;

import com.brandshield.brand.dto.AddKeywordsRequest;
import com.brandshield.brand.dto.BrandResponse;
import com.brandshield.brand.dto.CreateBrandRequest;
import com.brandshield.brand.dto.UpdateBrandRequest;
import com.brandshield.brand.service.BrandService;
import com.brandshield.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse createBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateBrandRequest request) {

        return brandService.createBrand(userDetails.getUser().getId(), request);
    }

    @GetMapping
    public Page<BrandResponse> getUserBrands(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return brandService.getUserBrands(userDetails.getUser().getId(), pageable);
    }

    @GetMapping("/{id}")
    public BrandResponse getBrandById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        return brandService.getBrandById(userDetails.getUser().getId(), id);
    }

    @PutMapping("/{id}")
    public BrandResponse updateBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandRequest request) {

        return brandService.updateBrand(userDetails.getUser().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        brandService.deleteBrand(userDetails.getUser().getId(), id);
    }

    @PostMapping("/{id}/keywords")
    public BrandResponse addKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AddKeywordsRequest request) {

        return brandService.addKeywords(userDetails.getUser().getId(), id, request);
    }

    @DeleteMapping("/{id}/keywords/{keyword}")
    public BrandResponse removeKeyword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable String keyword) {

        return brandService.removeKeyword(userDetails.getUser().getId(), id, keyword);
    }
}
