package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.user.NotaUserDetailsService
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionAuthenticationFilter(
    private val userDetailsService: NotaUserDetailsService,
) : OncePerRequestFilter() {
    private val logger = LoggerFactory.getLogger(SessionAuthenticationFilter::class.java)

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        // Check if user is already authenticated by Spring Security
        if (SecurityContextHolder.getContext().authentication?.isAuthenticated == true) {
            filterChain.doFilter(request, response)
            return
        }

        // Check for session-based authentication
        val session = request.getSession(false)
        val currentUser = session?.getAttribute("currentUser") as? UserDto

        if (currentUser != null) {
            // Rehydrate user details (authorities, enabled) from database
            val userDetails = runCatching {
                userDetailsService.loadUserByUsername(currentUser.username) as UserPrincipal
            }.onFailure {
                if (it is UsernameNotFoundException) {
                    logger.debug("Session 'currentUser' username not found anymore: {}", currentUser.username)
                } else {
                    logger.warn("Failed to load user details for session user {}: {}", currentUser.username, it.message)
                }
            }.getOrNull()

            if (userDetails != null && userDetails.isEnabled) {
                val authToken =
                    UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities,
                    )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isPublicEndpoint(uri: String): Boolean =
        uri.startsWith("/auth/") ||
            uri.startsWith("/api/auth/") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/actuator/")
}
