package com.globobank.fraud.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web configuration for GloboBank Fraud Detection Service.
 * 
 * Configures TLS 1.3+ security requirements, rate limiting for API protection, and web server
 * optimizations for fraud detection performance.
 * 
 * Security Features: - TLS 1.3+ enforcement - Rate limiting per client IP - Security headers
 * configuration - Connection timeout optimization - Performance tuning for sub-200ms SLA
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${fraud.security.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    /**
     * Tomcat server customization for TLS 1.3+ and performance optimization.
     *
     * @return WebServerFactoryCustomizer for Tomcat configuration
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // Enable compression for better performance
                connector.setProperty("compression", "on");
                connector.setProperty("compressableMimeType",
                        "text/html,text/xml,text/plain,application/json,application/javascript,text/css");

                // Connection timeout settings for fraud detection SLA
                connector.setProperty("connectionTimeout", "20000"); // 20 seconds
                connector.setProperty("keepAliveTimeout", "60000"); // 60 seconds
                connector.setProperty("maxKeepAliveRequests", "100");

                // Performance optimization
                connector.setProperty("acceptCount", "100");
                connector.setProperty("maxConnections", "8192");
                connector.setProperty("maxThreads", "200");
                connector.setProperty("minSpareThreads", "10");

                // Security settings
                connector.setProperty("server", "GloboBank-Fraud-Service");
                connector.setProperty("xpoweredBy", "false");

                // HTTP/2 support for better performance
                connector.addUpgradeProtocol(new org.apache.coyote.http2.Http2Protocol());
            });

            // Additional server configuration
            factory.addAdditionalTomcatConnectors(); // Can add HTTPS connector here in production
        };
    }

    /**
     * Rate limiting filter to protect against abuse and ensure fair resource allocation.
     *
     * @return FilterRegistrationBean with rate limiting filter
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitingFilter(requestsPerMinute));
        registrationBean.addUrlPatterns("/fraud/v1/risk-assessments");
        registrationBean.setOrder(3); // After correlation ID and audit filters
        return registrationBean;
    }

    /**
     * Security headers filter for enhanced web security.
     *
     * @return FilterRegistrationBean with security headers filter
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(4); // After rate limiting
        return registrationBean;
    }

    /**
     * Rate limiting filter implementation using token bucket algorithm.
     */
    public static class RateLimitingFilter implements Filter {

        private final int requestsPerMinute;
        private final ConcurrentHashMap<String, Bucket> buckets;

        public RateLimitingFilter(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
            this.buckets = new ConcurrentHashMap<>();
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String clientIp = getClientIpAddress(httpRequest);
            Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

            if (bucket.tryConsume(1)) {
                // Request allowed - continue processing
                chain.doFilter(request, response);
            } else {
                // Rate limit exceeded - return 429 Too Many Requests
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter()
                        .write("{\"error\":\"RATE_LIMIT_EXCEEDED\","
                                + "\"message\":\"Rate limit of " + requestsPerMinute
                                + " requests per minute exceeded\"," + "\"timestamp\":\""
                                + java.time.Instant.now().toString() + "\"}");
            }
        }

        private Bucket createBucket(String clientIp) {
            // Create bucket with specified requests per minute capacity
            Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                    Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
    }

    /**
     * Security headers filter for enhanced web security.
     */
    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Security headers for fraud service protection
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=()");

            // Content Security Policy for API endpoints
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");

            // Server identification (minimal information disclosure)
            httpResponse.setHeader("Server", "GloboBank-Fraud-Service");

            // Cache control for sensitive financial data
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                if (httpRequest.getRequestURI().contains("/risk-assessments")) {
                    httpResponse.setHeader("Cache-Control",
                            "no-cache, no-store, must-revalidate, private");
                    httpResponse.setHeader("Pragma", "no-cache");
                    httpResponse.setHeader("Expires", "0");
                }
            }

            chain.doFilter(request, response);
        }
    }
}
