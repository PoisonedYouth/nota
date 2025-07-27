package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class AdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var adminService: AdminService

    @BeforeEach
    fun setup() {
        adminService = mockk()
        val controller = AdminController(adminService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `showAdminOverview should return admin overview for admin user`() {
        // Given
        val adminUser = UserDto(id = 1L, username = "testuser", mustChangePassword = false, role = UserRole.ADMIN)
        val session = MockHttpSession()
        session.setAttribute("currentUser", adminUser)

        val userStatistics = listOf(
            AdminUserStatisticsDto(
                id = 1L,
                username = "testuser",
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                totalNotes = 0L,
                archivedNotes = 0L,
                sharedNotes = 0L,
                mustChangePassword = false,
                enabled = true,
            ),
            AdminUserStatisticsDto(
                id = 2L,
                username = "user1",
                createdAt = LocalDateTime.of(2024, 1, 2, 11, 0),
                totalNotes = 5L,
                archivedNotes = 2L,
                sharedNotes = 1L,
                mustChangePassword = true,
                enabled = true,
            ),
        )

        val systemStatistics = AdminSystemStatisticsDto(
            totalUsers = 2L,
            totalNotes = 5L,
            totalArchivedNotes = 2L,
            totalSharedNotes = 1L,
        )

        every { adminService.isAdmin("testuser") } returns true
        every { adminService.getAllUserStatistics() } returns userStatistics
        every { adminService.getSystemStatistics() } returns systemStatistics

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/overview")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("admin/overview"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("userStatistics"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("systemStatistics"))
            .andExpect(MockMvcResultMatchers.model().attribute("currentUser", adminUser))
            .andExpect(MockMvcResultMatchers.model().attribute("userStatistics", userStatistics))
            .andExpect(MockMvcResultMatchers.model().attribute("systemStatistics", systemStatistics))

        verify { adminService.isAdmin("testuser") }
        verify { adminService.getAllUserStatistics() }
        verify { adminService.getSystemStatistics() }
    }

    @Test
    fun `showAdminOverview should redirect to login when user is not authenticated`() {
        // Given
        val session = MockHttpSession()
        // No currentUser in session

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/overview")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `showAdminOverview should redirect to login when user is not admin`() {
        // Given
        val regularUser = UserDto(id = 2L, username = "user1", mustChangePassword = false, role = UserRole.USER)
        val session = MockHttpSession()
        session.setAttribute("currentUser", regularUser)

        every { adminService.isAdmin("user1") } returns false

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/overview")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))

        verify { adminService.isAdmin("user1") }
    }

    @Test
    fun `showAdminOverview should handle empty user statistics`() {
        // Given
        val adminUser = UserDto(id = 1L, username = "testuser", mustChangePassword = false, role = UserRole.ADMIN)
        val session = MockHttpSession()
        session.setAttribute("currentUser", adminUser)

        val emptyUserStatistics = emptyList<AdminUserStatisticsDto>()
        val systemStatistics = AdminSystemStatisticsDto(
            totalUsers = 0L,
            totalNotes = 0L,
            totalArchivedNotes = 0L,
            totalSharedNotes = 0L,
        )

        every { adminService.isAdmin("testuser") } returns true
        every { adminService.getAllUserStatistics() } returns emptyUserStatistics
        every { adminService.getSystemStatistics() } returns systemStatistics

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/overview")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("admin/overview"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("userStatistics"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("systemStatistics"))
            .andExpect(MockMvcResultMatchers.model().attribute("userStatistics", emptyUserStatistics))

        verify { adminService.isAdmin("testuser") }
        verify { adminService.getAllUserStatistics() }
        verify { adminService.getSystemStatistics() }
    }

    @Test
    fun `redirectToOverview should redirect to admin overview`() {
        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/admin"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/overview"))
    }

    @Test
    fun `admin access should work with different admin usernames if isAdmin logic changes`() {
        // Given - testing that the controller properly delegates admin check to service
        val customAdminUser = UserDto(id = 3L, username = "superadmin", mustChangePassword = false, role = UserRole.ADMIN)
        val session = MockHttpSession()
        session.setAttribute("currentUser", customAdminUser)

        val userStatistics = listOf<AdminUserStatisticsDto>()
        val systemStatistics = AdminSystemStatisticsDto(0L, 0L, 0L, 0L)

        every { adminService.isAdmin("superadmin") } returns true
        every { adminService.getAllUserStatistics() } returns userStatistics
        every { adminService.getSystemStatistics() } returns systemStatistics

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/overview")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("admin/overview"))

        verify { adminService.isAdmin("superadmin") }
    }
}
