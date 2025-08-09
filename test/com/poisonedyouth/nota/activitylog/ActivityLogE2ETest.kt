package com.poisonedyouth.nota.activitylog

import com.poisonedyouth.nota.notes.NoteService
import com.poisonedyouth.nota.user.UserService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
class ActivityLogE2ETest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var noteService: NoteService

    @Autowired
    private lateinit var activityLogService: ActivityLogService

    private lateinit var mockMvc: MockMvc

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `complete activity log workflow should work end to end`() {
        // Step 1: Login user (this should create a LOGIN activity)
        val session = MockHttpSession()
        val loginResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "TestPassword123!")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().is3xxRedirection)
                .andReturn()

        val user = session.getAttribute("currentUser") as com.poisonedyouth.nota.user.UserDto
        user shouldNotBe null

        // Step 2: Create a note (this should create a CREATE activity)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/notes/new")
                    .param("title", "Test Note")
                    .param("content", "This is a test note content")
                    .session(session),
            ).andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Get the created note to use its ID in subsequent steps
        val userNotes = noteService.findAllNotes(user.id)
        val createdNote = userNotes.first { it.title == "Test Note" }
        createdNote shouldNotBe null

        // Step 3: Update the note (this should create an UPDATE activity)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/notes/${createdNote.id}")
                    .param("title", "Updated Test Note")
                    .param("content", "Updated content")
                    .session(session),
            ).andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Step 4: Archive the note (this should create an ARCHIVE activity)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .delete("/notes/${createdNote.id}")
                    .session(session),
            ).andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Step 5: Check that all activities were logged (with retry for async processing)
        var activities = listOf<com.poisonedyouth.nota.activitylog.ActivityLogDto>()
        var attempts = 0
        val maxAttempts = 10
        while (attempts < maxAttempts && activities.size < 4) {
            Thread.sleep(100) // Wait 100ms between attempts
            activities = activityLogService.getAllActivities(user.id)
            attempts++
        }
        activities.size shouldBe 4 // LOGIN, CREATE, UPDATE, ARCHIVE

        // Verify the activities (most recent first)
        activities[0].action shouldBe "ARCHIVE"
        activities[0].description shouldBe "Note archived: 'Updated Test Note'"

        activities[1].action shouldBe "UPDATE"
        activities[1].description shouldBe "Note updated: 'Updated Test Note'"

        activities[2].action shouldBe "CREATE"
        activities[2].description shouldBe "Note created: 'Test Note'"

        activities[3].action shouldBe "LOGIN"
        activities[3].description shouldBe "User logged in"

        // Step 6: Access the activity log view
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/notes/activity-log")
                    .session(session),
            ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))

        // Step 7: Verify that the activity log link is present in the main notes view
        val result =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/notes")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.view().name("notes/list"))
                .andReturn()

        val content = result.response.contentAsString
        content.contains("Activity Log") shouldBe true
        content.contains("/notes/activity-log") shouldBe true
    }

    @Test
    fun `activity log should be accessible only to authenticated users`() {
        // Try to access activity log without authentication
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/activity-log"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `activity log should show user-specific activities only`() {
        // Create activities for user 1
        activityLogService.logActivity(
            userId = 1L,
            action = "CREATE",
            entityType = "NOTE",
            entityId = 123L,
            description = "User 1 created a note",
        )

        // Create activities for user 2
        activityLogService.logActivity(
            userId = 2L,
            action = "CREATE",
            entityType = "NOTE",
            entityId = 456L,
            description = "User 2 created a note",
        )

        // Wait a bit for potential async processing (though this test uses direct service calls)
        Thread.sleep(100)

        // Verify user 1 only sees their activities
        val user1Activities = activityLogService.getAllActivities(1L)
        user1Activities.size shouldBe 1
        user1Activities[0].description shouldBe "User 1 created a note"

        // Verify user 2 only sees their activities
        val user2Activities = activityLogService.getAllActivities(2L)
        user2Activities.size shouldBe 1
        user2Activities[0].description shouldBe "User 2 created a note"
    }

    @Test
    fun `activity log pagination should work correctly`() {
        // Step 1: Login user
        val session = MockHttpSession()
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/auth/login")
                .param("username", "testuser")
                .param("password", "TestPassword123!")
                .session(session),
        )

        val user = session.getAttribute("currentUser") as com.poisonedyouth.nota.user.UserDto

        // Step 2: Create many activities to trigger pagination
        repeat(25) { index ->
            activityLogService.logActivity(
                userId = user.id,
                action = "CREATE",
                entityType = "NOTE",
                entityId = index.toLong(),
                description = "Test activity $index",
            )
        }

        // Step 3: Test first page with pagination controls
        val firstPageResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/notes/activity-log")
                        .param("page", "0")
                        .param("size", "10")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentPage", 0))
                .andExpect(MockMvcResultMatchers.model().attribute("totalPages", 3))
                .andExpect(MockMvcResultMatchers.model().attribute("totalElements", 26L)) // 25 activities + 1 LOGIN activity
                .andExpect(MockMvcResultMatchers.model().attribute("hasNext", true))
                .andExpect(MockMvcResultMatchers.model().attribute("hasPrevious", false))
                .andReturn()

        val firstPageContent = firstPageResult.response.contentAsString
        // Verify pagination controls are present
        firstPageContent.contains("pagination-container") shouldBe true
        firstPageContent.contains("Page 1 of 3") shouldBe true
        firstPageContent.contains("26 activities total") shouldBe true
        firstPageContent.contains("Next →") shouldBe true
        // Previous button should be disabled on first page
        firstPageContent.contains("btn-secondary disabled") shouldBe true

        // Step 4: Test second page
        val secondPageResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/notes/activity-log")
                        .param("page", "1")
                        .param("size", "10")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentPage", 1))
                .andExpect(MockMvcResultMatchers.model().attribute("hasNext", true))
                .andExpect(MockMvcResultMatchers.model().attribute("hasPrevious", true))
                .andReturn()

        val secondPageContent = secondPageResult.response.contentAsString
        secondPageContent.contains("Page 2 of 3") shouldBe true
        secondPageContent.contains("← Previous") shouldBe true
        secondPageContent.contains("Next →") shouldBe true

        // Step 5: Test last page
        val lastPageResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/notes/activity-log")
                        .param("page", "2")
                        .param("size", "10")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.model().attribute("currentPage", 2))
                .andExpect(MockMvcResultMatchers.model().attribute("hasNext", false))
                .andExpect(MockMvcResultMatchers.model().attribute("hasPrevious", true))
                .andReturn()

        val lastPageContent = lastPageResult.response.contentAsString
        lastPageContent.contains("Page 3 of 3") shouldBe true
        lastPageContent.contains("← Previous") shouldBe true
        // Next button should be disabled on last page
        lastPageContent.contains("Next →") shouldBe true
        lastPageContent.contains("btn-secondary disabled") shouldBe true
    }

    @Test
    fun `activity log should not show pagination for single page`() {
        // Step 1: Login user
        val session = MockHttpSession()
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/auth/login")
                .param("username", "testuser")
                .param("password", "TestPassword123!")
                .session(session),
        )

        val user = session.getAttribute("currentUser") as com.poisonedyouth.nota.user.UserDto

        // Step 2: Create only a few activities (less than page size)
        repeat(5) { index ->
            activityLogService.logActivity(
                userId = user.id,
                action = "CREATE",
                entityType = "NOTE",
                entityId = index.toLong(),
                description = "Test activity $index",
            )
        }

        // Step 3: Verify pagination controls are not shown
        val result =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/notes/activity-log")
                        .param("size", "20")
                        .session(session),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.model().attribute("totalPages", 1))
                .andReturn()

        val content = result.response.contentAsString
        // Pagination container should not be present when totalPages <= 1
        content.contains("pagination-container") shouldBe false
        content.contains("Page 1 of 1") shouldBe false
    }
}
