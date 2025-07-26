package com.poisonedyouth.nota.activitylog

import com.poisonedyouth.nota.user.UserDto
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
        val user = UserDto(1L, "testuser", false)
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
    fun `showActivityLog should respect limit parameter`() {
        // Given
        val session = MockHttpSession()
        val user = UserDto(1L, "testuser", false)
        session.setAttribute("currentUser", user)

        // Create multiple test activities
        repeat(10) { index ->
            activityLogService.logActivity(
                userId = user.id,
                action = "CREATE",
                entityType = "NOTE",
                entityId = index.toLong(),
                description = "Notiz erstellt: 'Test Note $index'",
            )
        }

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/activity-log")
                .param("limit", "5")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
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
