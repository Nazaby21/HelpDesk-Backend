package com.zaby.helpdesk.config;

import com.zaby.helpdesk.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == "anonymousUser") {
                return Optional.empty();
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return Optional.of(userDetails.getUser().getId()); // match Long type of createdBy
        };
    }
}
