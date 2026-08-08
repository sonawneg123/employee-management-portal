package com.company.employeemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring Data JPA Auditing configuration.
 *
 * <p>Registers an {@link AuditorAware} bean named {@code "auditorAware"}
 * that is referenced by {@code @EnableJpaAuditing} on
 * {@link com.company.employeemanagement.EmployeeManagementApplication}.
 *
 * <p>The auditor is resolved from Spring Security's {@link SecurityContextHolder}
 * at the moment an entity is persisted or updated. If no authenticated principal
 * is present (e.g., during Flyway migration or anonymous requests),
 * {@code "SYSTEM"} is used as the fallback.
 *
 * @author Employee Management Portal Team
 */
@Configuration
public class AuditingConfig {

    /**
     * Returns an {@link AuditorAware} implementation that resolves the current
     * auditor from the Spring Security context.
     *
     * @return an {@link AuditorAware} producing the authenticated username or
     *         {@code "SYSTEM"} when no authentication is present
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(authentication.getName());
        };
    }
}
