package com.globobank.fraud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for GloboBank Fraud Detection Service.
 * 
 * Implements OAuth 2.0 + PKCE authentication with JWT token validation for secure API access and
 * fraud detection endpoints.
 * 
 * Security Features: - OAuth 2.0 Resource Server with JWT validation - PKCE (Proof Key for Code
 * Exchange) support - CORS configuration for cross-origin requests - Stateless session management -
 * Public health check endpoints - Role-based access control for fraud operations
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
@EnableWebSecurity
@Profile("!dev")
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${fraud.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${fraud.security.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${fraud.security.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${fraud.security.cors.max-age}")
    private Long maxAge;

    /**
     * Configures the main security filter chain with OAuth2 Resource Server.
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured for fraud service security
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF for REST API (stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints (no authentication required)
                        .requestMatchers("/fraud/v1/actuator/health").permitAll()
                        .requestMatchers("/fraud/v1/actuator/info").permitAll()

                        // Fraud assessment endpoints require fraud:assess scope
                        .requestMatchers("/fraud/v1/risk-assessments")
                        .hasAuthority("SCOPE_fraud:assess")

                        // Actuator endpoints require fraud:read scope
                        .requestMatchers("/fraud/v1/actuator/**").hasAuthority("SCOPE_fraud:read")

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))

                // Stateless session management (no sessions)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure headers for security
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny())
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000).includeSubDomains(true)))

                .build();
    }

    /**
     * JWT Decoder bean for validating JWT tokens from OAuth2 authorization server.
     * 
     * @return JwtDecoder configured with JWK Set URI
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * JWT Authentication Converter for extracting authorities from JWT claims.
     * 
     * @return JwtAuthenticationConverter configured for scope-based authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Configure authorities converter to extract scopes
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    /**
     * CORS configuration to allow cross-origin requests from approved domains.
     * 
     * @return CorsConfigurationSource with fraud service CORS policy
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Set allowed origins from configuration
        if (allowedOrigins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(allowedOrigins);
        }

        // Set allowed methods
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // Set allowed headers
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));

        // Allow credentials for authenticated requests
        configuration.setAllowCredentials(true);

        // Set max age for preflight requests
        configuration.setMaxAge(maxAge);

        // Expose correlation ID header for client tracking
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
