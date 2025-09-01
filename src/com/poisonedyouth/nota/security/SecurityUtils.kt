package com.poisonedyouth.nota.security

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserPrincipal
import com.poisonedyouth.nota.user.UserRole
import jakarta.servlet.http.HttpSession
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Security-related helpers.
 *
 * Provides a safe way to access the current authenticated user.
 *
 * Implementation note:
 * - Prefer Spring Security's principal (UserPrincipal) from SecurityContext.
 * - Fall back to legacy session attribute "currentUser" to keep tests and
 *   non-filtered controller usages working while migrating.
 */
object SecurityUtils {
    fun currentUser(session: HttpSession?): UserDto? {
        // 1) Try SecurityContext principal first
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? UserPrincipal
        if (principal != null && auth.isAuthenticated) {
            return UserDto(
                id = principal.id,
                username = principal.username,
                mustChangePassword = principal.mustChangePassword,
                role = extractRole(principal),
            )
        }

        // 2) Fallback to session attribute for compatibility
        val legacy = session?.getAttribute("currentUser")
        return legacy as? UserDto
    }

    private fun extractRole(principal: UserPrincipal): UserRole {
        // UserPrincipal already keeps the role internally but exposes only authorities publicly.
        // We cannot access role directly, so we infer from authorities or use username-based defaults if needed.
        // However, in our UserPrincipal we have a private role field; we can infer via authorities reliably.
        val hasAdmin = principal.authorities.any { it.authority == "ROLE_ADMIN" }
        return if (hasAdmin) UserRole.ADMIN else UserRole.USER
    }
}
