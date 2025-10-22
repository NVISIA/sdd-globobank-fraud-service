package com.globobank.fraud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Test Security configuration for GloboBank Fraud Detection Service.
 * 
 * This configuration is active only for the 'dev' profile and allows all requests without
 * authentication for testing purposes. This should NOT be used in production.
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
public class TestSecurityConfig {

    /**
     * Configures a permissive security filter chain for development/testing.
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured to permit all requests
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF for REST API (stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Allow all requests without authentication
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())

                // Stateless session management (no sessions)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure headers for security
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny())
                        .contentTypeOptions(contentTypeOptions -> {
                        }))

                .build();
    }

    /**
     * CORS configuration to allow cross-origin requests for testing.
     * 
     * @return CorsConfigurationSource with permissive CORS policy for testing
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins for testing
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials for authenticated requests
        configuration.setAllowCredentials(true);

        // Set max age for preflight requests
        configuration.setMaxAge(3600L);

        // Expose correlation ID header for client tracking
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
