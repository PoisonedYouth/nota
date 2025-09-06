package com.poisonedyouth.nota.common

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * Standardized error response model for API responses
 */
data class ApiErrorResponse(
    val error: ErrorDetail,
    val timestamp: LocalDateTime,
    val path: String,
    val method: String? = null,
    val requestId: String? = null,
) {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val formattedTimestamp: LocalDateTime = timestamp
}

/**
 * Core error detail information
 */
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: String? = null,
    val validationErrors: List<ValidationError>? = null,
    val cause: String? = null,
)

/**
 * Validation error for field-specific errors
 */
data class ValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String,
    val code: String? = null,
)

/**
 * Enumeration of common error codes for consistent error handling
 */
enum class ErrorCode(val code: String, val defaultMessage: String) {
    // Authentication & Authorization
    UNAUTHORIZED("AUTH_001", "Authentication required"),
    FORBIDDEN("AUTH_002", "Access denied"),
    INVALID_CREDENTIALS("AUTH_003", "Invalid username or password"),
    ACCOUNT_DISABLED("AUTH_004", "Account is disabled"),
    SESSION_EXPIRED("AUTH_005", "Session has expired"),

    // Validation Errors
    VALIDATION_FAILED("VAL_001", "Validation failed"),
    MISSING_REQUIRED_FIELD("VAL_002", "Required field is missing"),
    INVALID_FORMAT("VAL_003", "Invalid format"),
    VALUE_TOO_LONG("VAL_004", "Value exceeds maximum length"),
    VALUE_TOO_SHORT("VAL_005", "Value is below minimum length"),

    // Resource Errors
    RESOURCE_NOT_FOUND("RES_001", "Resource not found"),
    RESOURCE_ALREADY_EXISTS("RES_002", "Resource already exists"),
    RESOURCE_CONFLICT("RES_003", "Resource conflict"),
    OPTIMISTIC_LOCK_FAILURE("RES_004", "Resource was modified by another user"),

    // File Upload Errors
    FILE_TOO_LARGE("FILE_001", "File size exceeds maximum limit"),
    INVALID_FILE_TYPE("FILE_002", "Invalid file type"),
    FILE_UPLOAD_FAILED("FILE_003", "File upload failed"),
    UNSAFE_FILE_CONTENT("FILE_004", "File contains potentially unsafe content"),

    // Business Logic Errors
    NOTE_NOT_ACCESSIBLE("BIZ_001", "Note is not accessible"),
    NOTE_ALREADY_ARCHIVED("BIZ_002", "Note is already archived"),
    USER_REGISTRATION_FAILED("BIZ_003", "User registration failed"),
    PASSWORD_CHANGE_REQUIRED("BIZ_004", "Password change is required"),

    // System Errors
    INTERNAL_SERVER_ERROR("SYS_001", "Internal server error"),
    DATABASE_ERROR("SYS_002", "Database operation failed"),
    EXTERNAL_SERVICE_ERROR("SYS_003", "External service unavailable"),
    TIMEOUT_ERROR("SYS_004", "Operation timed out"),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded"),
    TOO_MANY_REQUESTS("RATE_002", "Too many requests"),
    ;

    fun toErrorDetail(
        message: String = defaultMessage,
        details: String? = null,
        validationErrors: List<ValidationError>? = null,
        cause: String? = null,
    ): ErrorDetail {
        return ErrorDetail(
            code = this.code,
            message = message,
            details = details,
            validationErrors = validationErrors,
            cause = cause,
        )
    }
}

/**
 * Custom exceptions with error codes
 */
abstract class NotaException(
    val errorCode: ErrorCode,
    message: String = errorCode.defaultMessage,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class NotaValidationException(
    message: String,
    val validationErrors: List<ValidationError> = emptyList(),
) : NotaException(ErrorCode.VALIDATION_FAILED, message)

class NotaResourceNotFoundException(
    message: String = ErrorCode.RESOURCE_NOT_FOUND.defaultMessage,
) : NotaException(ErrorCode.RESOURCE_NOT_FOUND, message)

class NotaUnauthorizedException(
    message: String = ErrorCode.UNAUTHORIZED.defaultMessage,
) : NotaException(ErrorCode.UNAUTHORIZED, message)

class NotaForbiddenException(
    message: String = ErrorCode.FORBIDDEN.defaultMessage,
) : NotaException(ErrorCode.FORBIDDEN, message)

class NotaConflictException(
    message: String = ErrorCode.RESOURCE_CONFLICT.defaultMessage,
) : NotaException(ErrorCode.RESOURCE_CONFLICT, message)

class NotaOptimisticLockException(
    message: String = ErrorCode.OPTIMISTIC_LOCK_FAILURE.defaultMessage,
) : NotaException(ErrorCode.OPTIMISTIC_LOCK_FAILURE, message)

class NotaFileUploadException(
    errorCode: ErrorCode,
    message: String = errorCode.defaultMessage,
) : NotaException(errorCode, message)

/**
 * Builder for creating consistent error responses
 */
class ApiErrorResponseBuilder(
    private val errorCode: ErrorCode,
    private val path: String,
    private val method: String? = null,
) {
    private var message: String = errorCode.defaultMessage
    private var details: String? = null
    private var validationErrors: List<ValidationError>? = null
    private var cause: String? = null
    private var requestId: String? = null
    private var timestamp: LocalDateTime = LocalDateTime.now()

    fun message(message: String) = apply { this.message = message }
    fun details(details: String) = apply { this.details = details }
    fun validationErrors(errors: List<ValidationError>) = apply { this.validationErrors = errors }
    fun cause(cause: String?) = apply { this.cause = cause }
    fun requestId(requestId: String) = apply { this.requestId = requestId }
    fun timestamp(timestamp: LocalDateTime) = apply { this.timestamp = timestamp }

    fun build(): ApiErrorResponse {
        return ApiErrorResponse(
            error = ErrorDetail(
                code = errorCode.code,
                message = message,
                details = details,
                validationErrors = validationErrors,
                cause = cause,
            ),
            timestamp = timestamp,
            path = path,
            method = method,
            requestId = requestId,
        )
    }
}

/**
 * Utility functions for error handling
 */
object ErrorUtils {

    fun createValidationError(field: String, value: Any?, message: String, code: String? = null): ValidationError {
        return ValidationError(field, value, message, code)
    }

    fun createApiErrorResponse(
        errorCode: ErrorCode,
        path: String,
        method: String? = null,
        message: String = errorCode.defaultMessage,
    ): ApiErrorResponse {
        return ApiErrorResponseBuilder(errorCode, path, method)
            .message(message)
            .build()
    }

    fun fromException(exception: NotaException, path: String, method: String? = null): ApiErrorResponse {
        val builder = ApiErrorResponseBuilder(exception.errorCode, path, method)
            .message(exception.message ?: exception.errorCode.defaultMessage)
            .cause(exception.cause?.message)

        if (exception is NotaValidationException) {
            builder.validationErrors(exception.validationErrors)
        }

        return builder.build()
    }
}
