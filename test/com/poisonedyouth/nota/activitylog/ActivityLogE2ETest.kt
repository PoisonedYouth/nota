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
        println("[DEBUG_LOG] Starting complete activity log workflow test")

        // Step 1: Login user (this should create a LOGIN activity)
        println("[DEBUG_LOG] Step 1: Logging in user")
        val session = MockHttpSession()
        val loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", "testuser")
                .param("password", "password")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andReturn()

        val user = session.getAttribute("currentUser") as com.poisonedyouth.nota.user.UserDto
        user shouldNotBe null

        // Step 2: Create a note (this should create a CREATE activity)
        println("[DEBUG_LOG] Step 2: Creating a note")
        mockMvc.perform(
            MockMvcRequestBuilders.post("/notes/new")
                .param("title", "Test Note")
                .param("content", "This is a test note content")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Get the created note to use its ID in subsequent steps
        val userNotes = noteService.findAllNotes(user.id)
        val createdNote = userNotes.first { it.title == "Test Note" }
        createdNote shouldNotBe null

        // Step 3: Update the note (this should create an UPDATE activity)
        println("[DEBUG_LOG] Step 3: Updating the note")
        mockMvc.perform(
            MockMvcRequestBuilders.put("/notes/${createdNote.id}")
                .param("title", "Updated Test Note")
                .param("content", "Updated content")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Step 4: Archive the note (this should create an ARCHIVE activity)
        println("[DEBUG_LOG] Step 4: Archiving the note")
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/notes/${createdNote.id}")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

        // Step 5: Check that all activities were logged
        println("[DEBUG_LOG] Step 5: Checking logged activities")
        val activities = activityLogService.getAllActivities(user.id)
        activities.size shouldBe 4 // LOGIN, CREATE, UPDATE, ARCHIVE

        // Verify the activities (most recent first)
        activities[0].action shouldBe "ARCHIVE"
        activities[0].description shouldBe "Notiz archiviert: 'Updated Test Note'"

        activities[1].action shouldBe "UPDATE"
        activities[1].description shouldBe "Notiz bearbeitet: 'Updated Test Note'"

        activities[2].action shouldBe "CREATE"
        activities[2].description shouldBe "Notiz erstellt: 'Test Note'"

        activities[3].action shouldBe "LOGIN"
        activities[3].description shouldBe "Benutzer angemeldet"

        // Step 6: Access the activity log view
        println("[DEBUG_LOG] Step 6: Accessing activity log view")
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/activity-log")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/activity-log"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("activities"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))

        // Step 7: Verify that the activity log link is present in the main notes view
        println("[DEBUG_LOG] Step 7: Checking activity log link in main view")
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/notes")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andReturn()

        val content = result.response.contentAsString
        content.contains("Aktivitätsprotokoll") shouldBe true
        content.contains("/notes/activity-log") shouldBe true

        println("[DEBUG_LOG] Activity log workflow test completed successfully")
    }

    @Test
    fun `activity log should be accessible only to authenticated users`() {
        println("[DEBUG_LOG] Testing activity log access control")

        // Try to access activity log without authentication
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/activity-log"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))

        println("[DEBUG_LOG] Access control test completed successfully")
    }

    @Test
    fun `activity log should show user-specific activities only`() {
        println("[DEBUG_LOG] Testing user-specific activity isolation")

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

        // Verify user 1 only sees their activities
        val user1Activities = activityLogService.getAllActivities(1L)
        user1Activities.size shouldBe 1
        user1Activities[0].description shouldBe "User 1 created a note"

        // Verify user 2 only sees their activities
        val user2Activities = activityLogService.getAllActivities(2L)
        user2Activities.size shouldBe 1
        user2Activities[0].description shouldBe "User 2 created a note"

        println("[DEBUG_LOG] User isolation test completed successfully")
    }
}
