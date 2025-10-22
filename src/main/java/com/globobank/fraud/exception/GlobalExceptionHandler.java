package com.globobank.fraud.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the GloboBank Fraud Service.
 * 
 * Provides centralized exception handling with structured error responses, correlation ID tracking,
 * and comprehensive audit logging for compliance.
 * 
 * Handles: - FraudServiceException and subclasses - Spring Security exceptions - Validation
 * exceptions - Generic runtime exceptions - Database and external service exceptions
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles FraudServiceException and returns structured error response.
     *
     * @param ex the FraudServiceException
     * @param request the web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(FraudServiceException.class)
    public ResponseEntity<ErrorResponse> handleFraudServiceException(FraudServiceException ex,
            WebRequest request) {
        String correlationId =
                ex.getCorrelationId() != null ? ex.getCorrelationId() : generateCorrelationId();

        logException(ex, correlationId, "FraudServiceException occurred");

        ErrorResponse errorResponse =
                ErrorResponse.builder().error(ex.getErrorCode()).message(ex.getMessage())
                        .timestamp(Instant.now().toString()).correlationId(correlationId).build();

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * Handles Spring Security authentication exceptions.
     *
     * @param ex the AuthenticationException
     * @param request the web request
     * @return ResponseEntity with authentication error
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
            WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Authentication failed");

        ErrorResponse errorResponse = ErrorResponse.builder().error("UNAUTHORIZED")
                .message("Valid OAuth 2.0 token required").timestamp(Instant.now().toString())
                .correlationId(correlationId).build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles Spring Security access denied exceptions.
     *
     * @param ex the AccessDeniedException
     * @param request the web request
     * @return ResponseEntity with authorization error
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
            WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Access denied");

        ErrorResponse errorResponse = ErrorResponse.builder().error("FORBIDDEN")
                .message("Insufficient permissions for this operation")
                .timestamp(Instant.now().toString()).correlationId(correlationId).build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles validation exceptions from @Valid annotations.
     *
     * @param ex the MethodArgumentNotValidException
     * @param request the web request
     * @return ResponseEntity with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Validation failed");

        // Collect validation errors
        Map<String, Object> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        String message = "Validation failed: " + validationErrors.toString();

        ErrorResponse errorResponse = ErrorResponse.builder().error("VALIDATION_ERROR")
                .message(message).timestamp(Instant.now().toString()).correlationId(correlationId)
                .details(validationErrors).build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex the IllegalArgumentException
     * @param request the web request
     * @return ResponseEntity with bad request error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
            WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Invalid argument provided");

        ErrorResponse errorResponse =
                ErrorResponse.builder().error("INVALID_ARGUMENT").message(ex.getMessage())
                        .timestamp(Instant.now().toString()).correlationId(correlationId).build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other runtime exceptions.
     *
     * @param ex the RuntimeException
     * @param request the web request
     * @return ResponseEntity with internal server error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex,
            WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Unexpected runtime exception occurred");

        ErrorResponse errorResponse = ErrorResponse.builder().error("INTERNAL_ERROR")
                .message("An unexpected error occurred during fraud assessment")
                .timestamp(Instant.now().toString()).correlationId(correlationId).build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles all other exceptions as a fallback.
     *
     * @param ex the Exception
     * @param request the web request
     * @return ResponseEntity with internal server error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String correlationId = generateCorrelationId();

        logException(ex, correlationId, "Unexpected exception occurred");

        ErrorResponse errorResponse = ErrorResponse.builder().error("INTERNAL_ERROR")
                .message("An unexpected error occurred during fraud assessment")
                .timestamp(Instant.now().toString()).correlationId(correlationId).build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Logs the exception with correlation ID for audit trail.
     *
     * @param ex the exception to log
     * @param correlationId the correlation ID for tracking
     * @param message the log message
     */
    private void logException(Exception ex, String correlationId, String message) {
        MDC.put("correlationId", correlationId);
        try {
            if (ex instanceof FraudServiceException) {
                // Log fraud service exceptions at WARN level (expected business exceptions)
                logger.warn("{}: {}", message, ex.getMessage(), ex);
            } else if (ex instanceof AuthenticationException
                    || ex instanceof AccessDeniedException) {
                // Log security exceptions at WARN level
                logger.warn("{}: {}", message, ex.getMessage());
            } else {
                // Log unexpected exceptions at ERROR level
                logger.error("{}: {}", message, ex.getMessage(), ex);
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Generates a new correlation ID for request tracking.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        String existingCorrelationId = MDC.get("correlationId");
        return existingCorrelationId != null ? existingCorrelationId
                : "req-" + UUID.randomUUID().toString();
    }

    /**
     * Error response DTO for structured error responses.
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        private String timestamp;
        private String correlationId;
        private Map<String, Object> details;

        private ErrorResponse() {}

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ErrorResponse errorResponse = new ErrorResponse();

            public Builder error(String error) {
                errorResponse.error = error;
                return this;
            }

            public Builder message(String message) {
                errorResponse.message = message;
                return this;
            }

            public Builder timestamp(String timestamp) {
                errorResponse.timestamp = timestamp;
                return this;
            }

            public Builder correlationId(String correlationId) {
                errorResponse.correlationId = correlationId;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                errorResponse.details = details;
                return this;
            }

            public ErrorResponse build() {
                return errorResponse;
            }
        }

        // Getters
        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}
