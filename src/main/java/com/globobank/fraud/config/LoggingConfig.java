package com.globobank.fraud.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logging configuration for GloboBank Fraud Detection Service.
 * 
 * Provides request/response logging with correlation ID tracking for audit compliance and
 * operational monitoring requirements.
 * 
 * Features: - Correlation ID generation and tracking - Request/response logging without sensitive
 * data - Performance timing for fraud detection SLA monitoring - Audit trail for compliance
 * requirements - Structured logging for analysis and alerting
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
public class LoggingConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Request logging filter that captures request details without sensitive data.
     *
     * @return CommonsRequestLoggingFilter configured for fraud service logging
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();

        // Configure what to log
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(false); // Disabled to prevent logging sensitive credit card
                                                // data
        loggingFilter.setIncludeHeaders(false); // Disabled to prevent logging Authorization headers
        loggingFilter.setMaxPayloadLength(0); // No payload logging for security

        // Configure log message format
        loggingFilter.setBeforeMessagePrefix("FRAUD_REQUEST_START: ");
        loggingFilter.setAfterMessagePrefix("FRAUD_REQUEST_END: ");
        loggingFilter.setBeforeMessageSuffix("");
        loggingFilter.setAfterMessageSuffix("");

        return loggingFilter;
    }

    /**
     * Correlation ID filter that generates and tracks correlation IDs for request tracing.
     *
     * @return FilterRegistrationBean with correlation ID filter
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.addUrlPatterns("/fraud/v1/*");
        registrationBean.setOrder(1); // Highest priority to ensure correlation ID is available for
                                      // all other filters
        return registrationBean;
    }

    /**
     * Audit logging filter for compliance and security monitoring.
     *
     * @return FilterRegistrationBean with audit logging filter
     */
    @Bean
    public FilterRegistrationBean<AuditLoggingFilter> auditLoggingFilter() {
        FilterRegistrationBean<AuditLoggingFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new AuditLoggingFilter());
        registrationBean.addUrlPatterns("/fraud/v1/risk-assessments");
        registrationBean.setOrder(2); // After correlation ID filter
        return registrationBean;
    }

    /**
     * Filter for generating and managing correlation IDs across requests.
     */
    public static class CorrelationIdFilter implements Filter {

        private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = "req-" + UUID.randomUUID().toString();
            }

            // Set correlation ID in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            try {
                // Add correlation ID to response headers
                httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

                // Log request start
                log.info("Request started: {} {} with correlation ID: {}", httpRequest.getMethod(),
                        httpRequest.getRequestURI(), correlationId);

                // Continue filter chain
                chain.doFilter(request, response);

                // Log request end
                log.info("Request completed: {} {} with correlation ID: {} - Status: {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), correlationId,
                        httpResponse.getStatus());

            } finally {
                // Always clear MDC to prevent memory leaks
                MDC.remove(CORRELATION_ID_MDC_KEY);
            }
        }
    }

    /**
     * Filter for audit logging of fraud detection operations.
     */
    public static class AuditLoggingFilter implements Filter {

        private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            long startTime = System.currentTimeMillis();
            String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

            try {
                // Log audit start
                auditLogger.info(
                        "FRAUD_ASSESSMENT_START - Method: {}, URI: {}, CorrelationId: {}, ClientIP: {}, UserAgent: {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), correlationId,
                        getClientIpAddress(httpRequest),
                        sanitizeUserAgent(httpRequest.getHeader("User-Agent")));

                // Continue filter chain
                chain.doFilter(request, response);

            } finally {
                long duration = System.currentTimeMillis() - startTime;

                // Log audit end with performance metrics
                auditLogger.info(
                        "FRAUD_ASSESSMENT_END - Method: {}, URI: {}, CorrelationId: {}, Status: {}, Duration: {}ms",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), correlationId,
                        httpResponse.getStatus(), duration);

                // Log performance warning if exceeding SLA
                if (duration > 200) {
                    auditLogger.warn(
                            "PERFORMANCE_SLA_VIOLATION - Duration: {}ms exceeds 200ms SLA target, CorrelationId: {}",
                            duration, correlationId);
                }
            }
        }

        /**
         * Extracts client IP address considering proxy headers.
         *
         * @param request the HTTP request
         * @return the client IP address
         */
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

        /**
         * Sanitizes User-Agent header to prevent log injection attacks.
         *
         * @param userAgent the User-Agent header value
         * @return sanitized User-Agent string
         */
        private String sanitizeUserAgent(String userAgent) {
            if (userAgent == null) {
                return "Unknown";
            }

            // Remove control characters and limit length for security
            return userAgent.replaceAll("[\\r\\n\\t]", "").substring(0,
                    Math.min(userAgent.length(), 200));
        }
    }
}
