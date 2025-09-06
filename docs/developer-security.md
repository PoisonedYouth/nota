# Developer Security & Model Guide

This document explains Nota's security model and related developer-facing policies to help contributors work safely and efficiently.

Contents
- Security model (roles, principal, method security)
- CSRF design
- Sorting parameters (allowed fields)
- Sanitization policy (rich text and uploads)
- Upload limits and configuration
- API error model

## Security model

Core pieces
- Roles: Two roles are used: USER and ADMIN (see src/com/poisonedyouth/nota/user/UserRole.kt).
- Principal: Spring Security UserDetails implementation is UserPrincipal, which exposes authorities ROLE_USER or ROLE_ADMIN based on the user role (src/com/poisonedyouth/nota/user/UserPrincipal.kt).
- Method security: EnableMethodSecurity is turned on in SecurityConfig. Admin REST endpoints are guarded with @PreAuthorize("hasRole('ADMIN')") (src/com/poisonedyouth/nota/admin/AdminRestController.kt).
- Session to SecurityContext: SessionAuthenticationFilter rebuilds the authentication from the legacy session attribute "currentUser" on each request, resolving full user details/authorities and placing them into the SecurityContext (src/com/poisonedyouth/nota/config/SessionAuthenticationFilter.kt).
- Helpers: SecurityUtils.currentUser(session) prefers the SecurityContext principal and falls back to the session attribute for compatibility (src/com/poisonedyouth/nota/security/SecurityUtils.kt).

Web security highlights
- Security filter chain (src/com/poisonedyouth/nota/config/SecurityConfig.kt):
  - Authenticated by default, with public endpoints under /auth/**, /api/auth/**, static resources, and /actuator/**.
  - Hardened headers including CSP, X-Frame-Options, and X-Content-Type-Options.
  - Session fixation protection and single concurrent session per user (configurable behavior).

## CSRF design

- CSRF tokens are managed by CookieCsrfTokenRepository with the following attributes (SecurityConfig):
  - Cookie name: XSRF-TOKEN (default Spring name)
  - Cookie flags: Secure, SameSite=Lax, and NOT HttpOnly so the frontend (e.g., HTMX) can read it.
- Endpoints exempt from CSRF:
  - /auth/** and /api/auth/** (for authentication flows)
  - Requests with header HX-Request: true (htmx progressive enhancement flows)
- Frontend usage:
  - Clients should read the XSRF-TOKEN cookie and send its value in the X-XSRF-TOKEN request header for state-mutating requests.
- Tests:
  - CookieSecurityIT verifies the CSRF cookie is Secure and not HttpOnly.

## Sorting parameters (allowed fields)

Several endpoints accept sort parameters (sort and order), defaulting to sort=updatedAt and order=desc. The service currently delegates the value directly to Spring's Sort.by.

Supported and recommended sort fields
- title
- createdAt
- updatedAt
- dueDate

Order values
- asc or desc (case-insensitive, any other value falls back to desc)

Notes
- Using unsupported fields may result in a persistence error at runtime. Callers should constrain sort to the supported list above.
- Files: NoteController and NoteRestController expose sort parameters; NoteService implements sorting via createSort.

## Sanitization policy

Rich text content
- Sanitization library: OWASP Java HTML Sanitizer (com.googlecode.owasp-java-html-sanitizer: 20240325.1), configured in module.yaml.
- Policy: NoteService combines Sanitizers.BLOCKS, Sanitizers.FORMATTING, and Sanitizers.LINKS.
  - Allowed: basic blocks, formatting, and safe links.
  - Disallowed: scripts, objects/embeds, event handlers, and dangerous URLs.
- Behavior: Input HTML is sanitized and trimmed on create and update. See NoteService.sanitizeHtmlContent and tests in RichTextFunctionalityTest and NoteSanitizationAndAccessTest.

Upload safety
- Filenames are sanitized to prevent traversal and unsafe characters (FileUploadSafetyValidator.sanitizeFilename).
- Content validation includes header content type checks and minimal magic byte sniffing for allowed types.

## Upload limits and configuration

Servlet container limits (resources/application.properties)
- spring.servlet.multipart.max-file-size = 25MB
- spring.servlet.multipart.max-request-size = 30MB

Application-level validator (FileUploadSafetyProperties)
- Default maxSizeBytes = 10 MB (10 * 1024 * 1024)
- Can be overridden via configuration property prefix nota.upload.maxSizeBytes.
- Allowed extensions (default): txt, md, pdf, png, jpg, jpeg, gif
- Allowed MIME types (default): text/plain, application/pdf, image/png, image/jpeg, image/gif

Error message
- GlobalExceptionHandler maps MaxUploadSizeExceededException to an end-user message "File too large! Maximum file size is 25MB." and returns an HTMX fragment or a redirect with a flash attribute based on the request type.

## API error model

Response structure
- ApiErrorResponse with fields:
  - error: ErrorDetail
    - code: stable symbolic code (see ErrorCode enum)
    - message: human-readable message
    - details: optional
    - validationErrors: optional list of field-level errors
    - cause: optional technical cause
  - timestamp: server time
  - path: request URI
  - method: HTTP method
  - requestId: optional (reserved)

Error codes
- Enumerated in ErrorCode (src/com/poisonedyouth/nota/common/ErrorModel.kt). Categories include AUTH, VAL, RES, FILE, BIZ, SYS, RATE.
- Mappings to HTTP status are defined in GlobalExceptionHandler.mapErrorCodeToHttpStatus.

Exception handling
- NotaException hierarchy allows throwing domain-specific errors that are converted to ApiErrorResponse via ErrorUtils.fromException.
- Validation errors are aggregated from MethodArgumentNotValidException, BindException, and ConstraintViolationException into validationErrors.
- Access denied (AccessDeniedException/AuthorizationDeniedException) is mapped to 403 with a standardized response.
- Unexpected exceptions are mapped to 500 with a generic message.

Examples
- 403 Forbidden
  {
    "error": { "code": "AUTH_002", "message": "Access is denied" },
    "path": "/api/admin/system-stats",
    "method": "GET",
    "timestamp": "yyyy-MM-dd HH:mm:ss"
  }

- 400 Validation failed
  {
    "error": {
      "code": "VAL_001",
      "message": "Validation failed for request",
      "validationErrors": [ { "field": "title", "message": "must not be blank" } ]
    },
    "path": "/api/notes",
    "method": "POST"
  }

## Developer tips

- Prefer SecurityUtils.currentUser(session) in controllers to get a consistent view of the user. In lower layers, prefer SecurityContext when available.
- Keep sort and filter parameters constrained to documented fields in controllers to provide predictable behavior.
- When changing sanitizer policy, update tests and (if needed) CSP in SecurityConfig.
- Adjust upload limits consistently across application.properties and FileUploadSafetyProperties to avoid user-facing mismatch.
- Ensure new exceptions extend NotaException where appropriate for consistent error responses.
