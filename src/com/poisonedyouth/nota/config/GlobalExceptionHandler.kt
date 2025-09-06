package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.common.ApiErrorResponse
import com.poisonedyouth.nota.common.ApiErrorResponseBuilder
import com.poisonedyouth.nota.common.ErrorCode
import com.poisonedyouth.nota.common.ErrorUtils
import com.poisonedyouth.nota.common.NotaException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest,
        model: Model,
        redirectAttributes: RedirectAttributes,
    ): String {
        val errorMessage = "File too large! Maximum file size is 25MB."

        // Check if this is an HTMX request
        val htmxRequest = request.getHeader("HX-Request")

        return if (htmxRequest != null) {
            // HTMX request - return error fragment
            model.addAttribute("error", errorMessage)
            "notes/fragments :: attachment-error"
        } else {
            // Regular request - redirect with flash attribute
            redirectAttributes.addFlashAttribute("error", errorMessage)
            "redirect:/notes"
        }
    }

    // ===== REST API Exception Handlers =====

    @ExceptionHandler(NotaException::class)
    fun handleNotaException(ex: NotaException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        logger.warn("Nota exception occurred: {} - {}", ex.errorCode.code, ex.message)

        val errorResponse = ErrorUtils.fromException(ex, request.requestURI, request.method)
        val httpStatus = mapErrorCodeToHttpStatus(ex.errorCode)

        return ResponseEntity.status(httpStatus).body(errorResponse)
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(
        ex: OptimisticLockingFailureException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Optimistic locking failure: {}", ex.message)

        val errorResponse = ErrorUtils.createApiErrorResponse(
            ErrorCode.OPTIMISTIC_LOCK_FAILURE,
            request.requestURI,
            request.method,
            "Resource was modified by another user. Please refresh and try again.",
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Validation failed: {}", ex.message)

        val validationErrors = ex.bindingResult.fieldErrors.map { fieldError ->
            ErrorUtils.createValidationError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                message = fieldError.defaultMessage ?: "Invalid value",
                code = fieldError.code,
            )
        }

        val errorResponse = ApiErrorResponseBuilder(
            ErrorCode.VALIDATION_FAILED,
            request.requestURI,
            request.method,
        )
            .message("Validation failed for request")
            .validationErrors(validationErrors)
            .build()

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        logger.warn("Binding failed: {}", ex.message)

        val validationErrors = ex.fieldErrors.map { fieldError ->
            ErrorUtils.createValidationError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                message = fieldError.defaultMessage ?: "Invalid value",
            )
        }

        val errorResponse = ApiErrorResponseBuilder(
            ErrorCode.VALIDATION_FAILED,
            request.requestURI,
            request.method,
        )
            .message("Request binding failed")
            .validationErrors(validationErrors)
            .build()

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Constraint violation: {}", ex.message)

        val validationErrors = ex.constraintViolations.map { violation ->
            ErrorUtils.createValidationError(
                field = violation.propertyPath.toString(),
                value = violation.invalidValue,
                message = violation.message,
            )
        }

        val errorResponse = ApiErrorResponseBuilder(
            ErrorCode.VALIDATION_FAILED,
            request.requestURI,
            request.method,
        )
            .message("Constraint validation failed")
            .validationErrors(validationErrors)
            .build()

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(
        org.springframework.security.access.AccessDeniedException::class,
        org.springframework.security.authorization.AuthorizationDeniedException::class,
    )
    fun handleAccessDenied(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Access denied: {}", ex.message)
        val errorResponse = ErrorUtils.createApiErrorResponse(
            ErrorCode.FORBIDDEN,
            request.requestURI,
            request.method,
            "Access is denied",
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected error occurred", ex)

        val errorResponse = ErrorUtils.createApiErrorResponse(
            ErrorCode.INTERNAL_SERVER_ERROR,
            request.requestURI,
            request.method,
            "An unexpected error occurred",
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    private fun mapErrorCodeToHttpStatus(errorCode: ErrorCode): HttpStatus {
        return when (errorCode) {
            ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, ErrorCode.SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED
            ErrorCode.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED -> HttpStatus.FORBIDDEN
            ErrorCode.RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCode.RESOURCE_ALREADY_EXISTS, ErrorCode.RESOURCE_CONFLICT, ErrorCode.OPTIMISTIC_LOCK_FAILURE -> HttpStatus.CONFLICT
            ErrorCode.VALIDATION_FAILED, ErrorCode.MISSING_REQUIRED_FIELD, ErrorCode.INVALID_FORMAT,
            ErrorCode.VALUE_TOO_LONG, ErrorCode.VALUE_TOO_SHORT, ErrorCode.INVALID_FILE_TYPE,
            ErrorCode.PASSWORD_CHANGE_REQUIRED,
            -> HttpStatus.BAD_REQUEST
            ErrorCode.FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE
            ErrorCode.RATE_LIMIT_EXCEEDED, ErrorCode.TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS
            ErrorCode.TIMEOUT_ERROR -> HttpStatus.REQUEST_TIMEOUT
            ErrorCode.EXTERNAL_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
