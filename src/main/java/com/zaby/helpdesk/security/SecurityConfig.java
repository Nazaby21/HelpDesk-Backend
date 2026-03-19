package com.zaby.helpdesk.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return auth.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // allow CORS
                .csrf(csrf -> csrf.disable()) // disable CSRF because API is stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Open endpoints (login)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/debug/**").permitAll()

                        // USER profile endpoint
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()

                        // User management - only ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")

                        // Ticket endpoints - note: plural /tickets not /ticket
                        .requestMatchers("/api/v1/tickets").hasAnyRole("ADMIN", "USER", "TECHNICIAN")
                        .requestMatchers("/api/v1/tickets/**").hasAnyRole("ADMIN", "USER", "TECHNICIAN")

                        // Department endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/departments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/departments/**").authenticated()
                        .requestMatchers("/api/v1/departments/**").hasRole("ADMIN")

                        // Category endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").authenticated()
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("ADMIN", "TECHNICIAN")

                        // WebSocket handshake endpoint - must be open
                        .requestMatchers("/ws-chat/**").permitAll()

                        // Allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Disable HTTP Basic Auth - JWT is used instead (Basic Auth causes browser popup)
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}