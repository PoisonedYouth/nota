package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder

class SessionAuthenticationFilterTest {

    private lateinit var sessionAuthenticationFilter: SessionAuthenticationFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain
    private lateinit var session: HttpSession

    @BeforeEach
    fun setup() {
        sessionAuthenticationFilter = SessionAuthenticationFilter()
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)
        session = mockk(relaxed = true)

        // Clear security context before each test
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should skip authentication for public auth endpoints`() {
        // Given
        every { request.requestURI } returns "/auth/login"

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
        // Filter chain should continue without setting authentication
    }

    @Test
    fun `should skip authentication for static resources`() {
        // Given
        every { request.requestURI } returns "/css/main.css"

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
    }

    @Test
    fun `should skip authentication for actuator endpoints`() {
        // Given
        every { request.requestURI } returns "/actuator/health"

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
    }

    @Test
    fun `should set authentication when user exists in session`() {
        // Given
        val userDto = UserDto(
            id = 1L,
            username = "testuser",
            mustChangePassword = false,
            role = UserRole.USER,
        )

        every { request.requestURI } returns "/notes"
        every { request.getSession(false) } returns session
        every { session.getAttribute("currentUser") } returns userDto

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        authentication shouldNotBe null
        authentication!!.name shouldBe "testuser"
        authentication.isAuthenticated shouldBe true
    }

    @Test
    fun `should not set authentication when no session exists`() {
        // Given
        every { request.requestURI } returns "/notes"
        every { request.getSession(false) } returns null

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
    }

    @Test
    fun `should not set authentication when no user in session`() {
        // Given
        every { request.requestURI } returns "/notes"
        every { request.getSession(false) } returns session
        every { session.getAttribute("currentUser") } returns null

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
    }

    @Test
    fun `should skip when already authenticated`() {
        // Given
        val existingAuth = mockk<org.springframework.security.core.Authentication>()
        every { existingAuth.isAuthenticated } returns true
        SecurityContextHolder.getContext().authentication = existingAuth

        every { request.requestURI } returns "/notes"

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe existingAuth
    }

    @Test
    fun `should handle invalid user object in session gracefully`() {
        // Given
        every { request.requestURI } returns "/notes"
        every { request.getSession(false) } returns session
        every { session.getAttribute("currentUser") } returns "invalid_user_object"

        // When
        sessionAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        SecurityContextHolder.getContext().authentication shouldBe null
    }
}
