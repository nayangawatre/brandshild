package com.brandshield.intelligence;

import com.brandshield.brand.entity.Brand;
import com.brandshield.brand.repository.BrandRepository;
import com.brandshield.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DomainIntelligenceService {

    private final DnsVerificationService dnsService;
    private final RdapLookupService rdapService;
    private final WebsiteContentAnalysisService contentAnalysisService;
    private final PlaywrightRendererService rendererService;
    private final CertificateTransparencyService ctService;
    private final BrandRepository brandRepository;

    public DomainIntelligenceService(
            DnsVerificationService dnsService,
            RdapLookupService rdapService,
            WebsiteContentAnalysisService contentAnalysisService,
            PlaywrightRendererService rendererService,
            CertificateTransparencyService ctService,
            BrandRepository brandRepository) {
        this.dnsService = dnsService;
        this.rdapService = rdapService;
        this.contentAnalysisService = contentAnalysisService;
        this.rendererService = rendererService;
        this.ctService = ctService;
        this.brandRepository = brandRepository;
    }

    public DomainIntelligenceDto getFullIntelligence(UUID userId, UUID brandId, String targetDomain) {
        Brand brand = brandRepository.findByIdAndUserId(brandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        // 1. DNS check
        var dnsResult = dnsService.verifyDomain(targetDomain);

        // 2. RDAP lookup
        var rdapResult = rdapService.lookupDomain(targetDomain);

        // 3. Jsoup Content Analysis
        var contentResult = contentAnalysisService.analyzeWebsite(
                targetDomain,
                brand.getOfficialWebsite(),
                brand.getName()
        );

        // 4. Playwright Screenshot & Rendering
        var renderResult = rendererService.captureSnapshot(targetDomain);

        // 5. Certificate Transparency Log Search
        Set<String> officialDomains = brand.getOfficialDomains() != null ? brand.getOfficialDomains() : Set.of();
        List<CertificateTransparencyService.CtCertificateEntry> ctFindings = ctService.searchCertificates(
                brand.getName(),
                officialDomains
        );

        return new DomainIntelligenceDto(
                targetDomain,
                dnsResult,
                rdapResult,
                contentResult,
                renderResult,
                ctFindings
        );
    }
}
