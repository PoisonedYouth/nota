package com.poisonedyouth.nota.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException

class NotaUserDetailsServiceTest {

    @Test
    fun `should load user and map to UserPrincipal`() {
        // Given
        val repo = mockk<UserRepository>()
        val service = NotaUserDetailsService(repo)
        val entity = User(
            id = 42L,
            username = "alice",
            password = "hash",
            mustChangePassword = false,
            role = UserRole.ADMIN,
            enabled = true,
        )
        every { repo.findByUsername("alice") } returns entity

        // When
        val details = service.loadUserByUsername("alice") as UserPrincipal

        // Then
        details.username shouldBe "alice"
        details.isEnabled shouldBe true
        details.authorities.map { it.authority } shouldBe listOf("ROLE_ADMIN")
    }

    @Test
    fun `should throw when user not found`() {
        // Given
        val repo = mockk<UserRepository>()
        val service = NotaUserDetailsService(repo)
        every { repo.findByUsername("missing") } returns null

        // When / Then
        shouldThrow<UsernameNotFoundException> {
            service.loadUserByUsername("missing")
        }
    }
}
