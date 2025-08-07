package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionAuthenticationFilter : OncePerRequestFilter() {
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
            // Create Spring Security authentication token
            val authToken =
                UsernamePasswordAuthenticationToken(
                    currentUser.username,
                    null,
                    emptyList(), // No authorities for now
                )
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain.doFilter(request, response)
    }

    private fun isPublicEndpoint(uri: String): Boolean =
        uri.startsWith("/auth/") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/actuator/")
}
