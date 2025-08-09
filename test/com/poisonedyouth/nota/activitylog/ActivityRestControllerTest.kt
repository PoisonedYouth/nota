package com.poisonedyouth.nota.activitylog

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ActivityRestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var activityLogService: ActivityLogService

    @BeforeEach
    fun setup() {
        activityLogService = mockk()
        val controller = ActivityRestController(activityLogService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun session(): MockHttpSession = MockHttpSession().apply {
        setAttribute("currentUser", UserDto(1L, "user", false, UserRole.USER))
    }

    @Test
    fun `should return recent activities`() {
        every { activityLogService.getRecentActivities(1L, 20) } returns emptyList()

        mockMvc
            .perform(get("/api/activity/recent").session(session()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activities").isArray)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `should return activities page`() {
        val page: Page<ActivityLogDto> = PageImpl(emptyList())
        every { activityLogService.getActivitiesPage(1L, 0, 20) } returns page

        mockMvc
            .perform(get("/api/activity").session(session()))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all activities`() {
        every { activityLogService.getAllActivities(1L) } returns emptyList()

        mockMvc
            .perform(get("/api/activity/all").session(session()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activities").isArray)
            .andExpect(jsonPath("$.count").value(0))
    }
}
