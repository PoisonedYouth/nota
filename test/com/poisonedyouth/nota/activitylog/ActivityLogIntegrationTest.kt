package com.poisonedyouth.nota.activitylog

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityLogIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var activityLogService: ActivityLogService

    private lateinit var mockMvc: MockMvc

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `showActivityLog should return activity log view for authenticated user`() {
        // Given
        val session = MockHttpSession()
        val user = UserDto(1L, "testuser", false, UserRole.USER)
        session.setAttribute("currentUser", user)

        // Create some test activities
        activityLogService.logActivity(
            userId = user.id,
            action = "LOGIN",
            entityType = "USER",
            entityId = user.id,
            description = "Benutzer angemeldet",
        )
        activityLogService.logActivity(
            userId = user.id,
            action = "CREATE",
            entityType = "NOTE",
            entityId = 123L,
            description = "Notiz erstellt: 'Test Note'",
        )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/activity-log")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
    }

    @Test
    fun `showActivityLog should redirect to login for unauthenticated user`() {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/activity-log"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `showActivityLog should respect page and size parameters`() {
        // Given
        val session = MockHttpSession()
        val user = UserDto(1L, "testuser", false, UserRole.USER)
        session.setAttribute("currentUser", user)

        // Create multiple test activities
        repeat(25) { index ->
            activityLogService.logActivity(
                userId = user.id,
                action = "CREATE",
                entityType = "NOTE",
                entityId = index.toLong(),
                description = "Notiz erstellt: 'Test Note $index'",
            )
        }

        // When & Then - Test first page
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/activity-log")
                .param("page", "0")
                .param("size", "10")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
            .andExpect(MockMvcResultMatchers.model().attribute("currentPage", 0))
            .andExpect(MockMvcResultMatchers.model().attribute("pageSize", 10))
            .andExpect(MockMvcResultMatchers.model().attribute("totalPages", 3))
            .andExpect(MockMvcResultMatchers.model().attribute("totalElements", 25L))
            .andExpect(MockMvcResultMatchers.model().attribute("hasNext", true))
            .andExpect(MockMvcResultMatchers.model().attribute("hasPrevious", false))
    }

    @Test
    fun `showActivityLog should handle second page correctly`() {
        // Given
        val session = MockHttpSession()
        val user = UserDto(1L, "testuser", false, UserRole.USER)
        session.setAttribute("currentUser", user)

        // Create multiple test activities
        repeat(25) { index ->
            activityLogService.logActivity(
                userId = user.id,
                action = "CREATE",
                entityType = "NOTE",
                entityId = index.toLong(),
                description = "Notiz erstellt: 'Test Note $index'",
            )
        }

        // When & Then - Test second page
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/activity-log")
                .param("page", "1")
                .param("size", "10")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
            .andExpect(MockMvcResultMatchers.model().attribute("currentPage", 1))
            .andExpect(MockMvcResultMatchers.model().attribute("pageSize", 10))
            .andExpect(MockMvcResultMatchers.model().attribute("totalPages", 3))
            .andExpect(MockMvcResultMatchers.model().attribute("totalElements", 25L))
            .andExpect(MockMvcResultMatchers.model().attribute("hasNext", true))
            .andExpect(MockMvcResultMatchers.model().attribute("hasPrevious", true))
    }

    @Test
    fun `activity logging should work for note operations`() {
        // Given
        val userId = 1L

        // When
        activityLogService.logActivity(
            userId = userId,
            action = "CREATE",
            entityType = "NOTE",
            entityId = 123L,
            description = "Notiz erstellt: 'Test Note'",
        )

        activityLogService.logActivity(
            userId = userId,
            action = "UPDATE",
            entityType = "NOTE",
            entityId = 123L,
            description = "Notiz bearbeitet: 'Updated Test Note'",
        )

        activityLogService.logActivity(
            userId = userId,
            action = "SHARE",
            entityType = "NOTE",
            entityId = 123L,
            description = "Notiz geteilt: 'Test Note' mit Benutzer 'otheruser'",
        )

        activityLogService.logActivity(
            userId = userId,
            action = "ARCHIVE",
            entityType = "NOTE",
            entityId = 123L,
            description = "Notiz archiviert: 'Test Note'",
        )

        // Then
        val activities = activityLogService.getAllActivities(userId)
        activities.size shouldBe 4
        activities[0].action shouldBe "ARCHIVE" // Most recent first
        activities[1].action shouldBe "SHARE"
        activities[2].action shouldBe "UPDATE"
        activities[3].action shouldBe "CREATE"
    }
}
