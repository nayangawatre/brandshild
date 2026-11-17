package com.brandshield.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.util.*;

@Service
public class DnsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(DnsVerificationService.class);

    public record DnsResult(
            boolean isResolving,
            List<String> ipAddresses,
            List<String> mxRecords,
            String status
    ) {}

    /**
     * Resolves A/AAAA and MX records for a domain to verify active network presence.
     */
    public DnsResult verifyDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return new DnsResult(false, List.of(), List.of(), "INVALID_DOMAIN");
        }

        String cleanDomain = cleanDomainString(domain);
        List<String> ipList = new ArrayList<>();
        List<String> mxList = new ArrayList<>();

        // 1. Resolve IP Addresses (A / AAAA)
        try {
            InetAddress[] addresses = InetAddress.getAllByName(cleanDomain);
            for (InetAddress addr : addresses) {
                ipList.add(addr.getHostAddress());
            }
        } catch (Exception e) {
            log.debug("DNS A-record resolution failed for {}: {}", cleanDomain, e.getMessage());
        }

        // 2. Resolve Mail Exchange (MX) records
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(cleanDomain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    mxList.add(attr.get(i).toString());
                }
            }
        } catch (Exception ignored) {
        }

        boolean resolving = !ipList.isEmpty();
        String status = resolving ? "ACTIVE_HOST" : "UNRESOLVED_NXDOMAIN";

        return new DnsResult(resolving, ipList, mxList, status);
    }

    private String cleanDomainString(String domain) {
        return domain.trim()
                .toLowerCase()
                .replaceFirst("^(https?://)?(www\\.)?", "")
                .split("/")[0]
                .split(":")[0];
    }
}
