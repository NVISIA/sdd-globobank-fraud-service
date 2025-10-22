package com.globobank.fraud.exception;

/**
 * Base exception class for all GloboBank Fraud Service exceptions.
 * 
 * Provides common exception handling infrastructure with correlation ID support, structured error
 * responses, and audit logging for compliance requirements.
 * 
 * Exception Categories: - Validation errors (400 Bad Request) - Authentication errors (401
 * Unauthorized) - Authorization errors (403 Forbidden) - Business logic errors (422 Unprocessable
 * Entity) - System errors (500 Internal Server Error) - External service errors (503 Service
 * Unavailable)
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
public class FraudServiceException extends RuntimeException {

    private final String errorCode;
    private final String correlationId;
    private final int httpStatus;

    /**
     * Constructs a new fraud service exception with the specified detail message.
     *
     * @param message the detail message
     */
    public FraudServiceException(String message) {
        this(message, "GENERAL_ERROR", null, 500);
    }

    /**
     * Constructs a new fraud service exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public FraudServiceException(String message, Throwable cause) {
        this(message, "GENERAL_ERROR", null, 500, cause);
    }

    /**
     * Constructs a new fraud service exception with full error details.
     *
     * @param message the detail message
     * @param errorCode the specific error code for this exception
     * @param correlationId the correlation ID for request tracking
     * @param httpStatus the HTTP status code to return
     */
    public FraudServiceException(String message, String errorCode, String correlationId,
            int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.httpStatus = httpStatus;
    }

    /**
     * Constructs a new fraud service exception with full error details and cause.
     *
     * @param message the detail message
     * @param errorCode the specific error code for this exception
     * @param correlationId the correlation ID for request tracking
     * @param httpStatus the HTTP status code to return
     * @param cause the cause
     */
    public FraudServiceException(String message, String errorCode, String correlationId,
            int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.httpStatus = httpStatus;
    }

    /**
     * Gets the error code for this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the correlation ID for request tracking.
     *
     * @return the correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the HTTP status code for this exception.
     *
     * @return the HTTP status code
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Creates a validation error exception.
     *
     * @param message the validation error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for validation errors
     */
    public static FraudServiceException validationError(String message, String correlationId) {
        return new FraudServiceException(message, "VALIDATION_ERROR", correlationId, 400);
    }

    /**
     * Creates an authentication error exception.
     *
     * @param message the authentication error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for authentication errors
     */
    public static FraudServiceException authenticationError(String message, String correlationId) {
        return new FraudServiceException(message, "AUTHENTICATION_ERROR", correlationId, 401);
    }

    /**
     * Creates an authorization error exception.
     *
     * @param message the authorization error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for authorization errors
     */
    public static FraudServiceException authorizationError(String message, String correlationId) {
        return new FraudServiceException(message, "AUTHORIZATION_ERROR", correlationId, 403);
    }

    /**
     * Creates a business logic error exception.
     *
     * @param message the business logic error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for business logic errors
     */
    public static FraudServiceException businessLogicError(String message, String correlationId) {
        return new FraudServiceException(message, "BUSINESS_LOGIC_ERROR", correlationId, 422);
    }

    /**
     * Creates a database error exception with fail-safe behavior.
     *
     * @param message the database error message
     * @param correlationId the correlation ID for request tracking
     * @param cause the underlying database exception
     * @return FraudServiceException configured for database errors
     */
    public static FraudServiceException databaseError(String message, String correlationId,
            Throwable cause) {
        return new FraudServiceException(message, "DATABASE_ERROR", correlationId, 500, cause);
    }

    /**
     * Creates an external service error exception.
     *
     * @param message the external service error message
     * @param correlationId the correlation ID for request tracking
     * @param cause the underlying service exception
     * @return FraudServiceException configured for external service errors
     */
    public static FraudServiceException externalServiceError(String message, String correlationId,
            Throwable cause) {
        return new FraudServiceException(message, "EXTERNAL_SERVICE_ERROR", correlationId, 503,
                cause);
    }

    /**
     * Creates a rate limit exceeded error exception.
     *
     * @param message the rate limit error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for rate limit errors
     */
    public static FraudServiceException rateLimitError(String message, String correlationId) {
        return new FraudServiceException(message, "RATE_LIMIT_EXCEEDED", correlationId, 429);
    }

    /**
     * Creates a service unavailable error exception.
     *
     * @param message the service unavailable error message
     * @param correlationId the correlation ID for request tracking
     * @return FraudServiceException configured for service unavailable errors
     */
    public static FraudServiceException serviceUnavailableError(String message,
            String correlationId) {
        return new FraudServiceException(message, "SERVICE_UNAVAILABLE", correlationId, 503);
    }

    @Override
    public String toString() {
        return String.format(
                "FraudServiceException{errorCode='%s', correlationId='%s', httpStatus=%d, message='%s'}",
                errorCode, correlationId, httpStatus, getMessage());
    }
}
