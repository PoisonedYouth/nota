package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRestControllerAuthorizationIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    // Mock the AdminService to avoid depending on other repositories
    @MockBean
    private lateinit var adminService: AdminService

    @BeforeEach
    fun cleanUsers() {
        // Ensure repository is empty before each test
        userRepository.deleteAll()
    }

    private fun persistUser(
        username: String,
        role: UserRole,
        enabled: Boolean = true,
    ): User {
        val user = User(
            username = username,
            password = "hash",
            mustChangePassword = false,
            role = role,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = enabled,
        )
        return userRepository.save(user)
    }

    private fun sessionFor(user: User): MockHttpSession = MockHttpSession().apply {
        setAttribute(
            "currentUser",
            UserDto(
                id = user.id ?: -1L,
                username = user.username,
                mustChangePassword = user.mustChangePassword,
                role = user.role,
            ),
        )
    }

    @Test
    fun `non-admin is forbidden by method security`() {
        // Given: a persisted normal user
        val normal = persistUser("normal_user_sec", UserRole.USER)

        // When & Then: accessing admin endpoint yields 403
        mockMvc
            .perform(
                get("/api/admin/system-stats").session(sessionFor(normal)).contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin is allowed by method security`() {
        // Given: a persisted admin user
        val admin = persistUser("admin_user_sec", UserRole.ADMIN)
        // Ensure the in-controller ensureAdmin passes too
        org.mockito.BDDMockito.given(adminService.isAdmin("admin_user_sec")).willReturn(true)

        // When & Then: accessing admin endpoint yields 200 (AdminService is mocked; default 200 with empty body)
        mockMvc
            .perform(
                get("/api/admin/system-stats").session(sessionFor(admin)).contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
    }
}
