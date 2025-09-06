package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AdminRestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var adminService: AdminService

    @BeforeEach
    fun setup() {
        adminService = mockk()
        val activityEventPublisher = mockk<com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher>(relaxed = true)
        val controller = AdminRestController(adminService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun adminSession(): MockHttpSession = MockHttpSession().apply {
        setAttribute("currentUser", UserDto(1L, "admin", false, UserRole.ADMIN))
    }

    private fun userSession(): MockHttpSession = MockHttpSession().apply {
        setAttribute("currentUser", UserDto(2L, "user", false, UserRole.USER))
    }

    @Test
    fun `should forbid non-admin for stats`() {
        every { adminService.isAdmin("user") } returns false
        mockMvc
            .perform(get("/api/admin/system-stats").session(userSession()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return system stats for admin`() {
        every { adminService.isAdmin("admin") } returns true
        every { adminService.getSystemStatistics() } returns AdminSystemStatisticsDto(1, 2, 3, 4)

        mockMvc
            .perform(get("/api/admin/system-stats").session(adminSession()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalUsers").value(1))
    }

    @Test
    fun `should enable and disable user`() {
        every { adminService.isAdmin("admin") } returns true
        every { adminService.disableUser(5L) } returns true
        every { adminService.enableUser(5L) } returns true

        mockMvc
            .perform(post("/api/admin/users/5/disable").session(adminSession()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User has been disabled successfully"))

        mockMvc
            .perform(post("/api/admin/users/5/enable").session(adminSession()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User has been enabled successfully"))
    }
}
