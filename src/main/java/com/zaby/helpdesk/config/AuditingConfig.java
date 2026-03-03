package com.zaby.helpdesk.config;

import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {
    @Bean
    public AuditorAware<User> auditorProvider(UserRepository userRepository) {
        return () -> {
            // return any existing user as dummy for now
            User user = userRepository.findById(1L).orElse(null);
            return Optional.ofNullable(user);
        };
    }
}
