package com.brandshield.intelligence;

import java.util.List;

public record DomainIntelligenceDto(
        String domain,
        DnsVerificationService.DnsResult dns,
        RdapLookupService.RdapResult rdap,
        WebsiteContentAnalysisService.WebsiteAnalysisResult contentAnalysis,
        PlaywrightRendererService.RenderedPageSnapshot rendering,
        List<CertificateTransparencyService.CtCertificateEntry> certificateTransparencyFindings
) {}
