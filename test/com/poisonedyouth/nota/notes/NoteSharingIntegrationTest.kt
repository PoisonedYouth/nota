package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoteSharingIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val noteRepository: NoteRepository,
        private val noteShareRepository: NoteShareRepository,
    ) {

        private lateinit var testUser: User
        private lateinit var targetUser: User
        private lateinit var testNote: Note
        private lateinit var session: MockHttpSession

        @BeforeEach
        fun setup() {
            // Create test users with unique usernames to avoid conflicts
            val timestamp = System.currentTimeMillis()
            testUser = userRepository.save(
                User(
                    username = "testuser_integration_$timestamp",
                    password = "password",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            targetUser = userRepository.save(
                User(
                    username = "targetuser_integration_$timestamp",
                    password = "password",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Create test note
            testNote = noteRepository.save(
                Note(
                    title = "Test Note",
                    content = "Test content for sharing",
                    user = testUser,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Create session with authenticated user
            session = MockHttpSession()
            val testUserDto = UserDto(
                id = testUser.id!!,
                username = testUser.username,
                mustChangePassword = false,
            )
            session.setAttribute("currentUser", testUserDto)
        }

        @AfterEach
        fun cleanup() {
            noteShareRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()
        }

        @Test
        fun `should display share modal for note owner`() {
            mockMvc.perform(
                get("/notes/modal/${testNote.id}")
                    .param("mode", "share")
                    .session(session),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz teilen: Test Note")))
                .andExpect(content().string(containsString("Notiz teilen")))
        }

        @Test
        fun `should redirect when note not found for share modal`() {
            mockMvc.perform(
                get("/notes/modal/999")
                    .param("mode", "share")
                    .session(session),
            )
                .andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("/notes"))
        }

        @Test
        fun `should redirect when user not authenticated for share modal`() {
            mockMvc.perform(
                get("/notes/modal/${testNote.id}")
                    .param("mode", "share"),
            )
                .andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("/auth/login"))
        }

        @Test
        fun `should successfully share note with valid user`() {
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", targetUser.username)
                    .param("permission", "read")
                    .session(session)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz erfolgreich geteilt!")))

            // Verify share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 1
            shares[0].sharedWithUser.username shouldBe targetUser.username
        }

        @Test
        fun `should fail when sharing with non-existent user`() {
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", "nonexistent")
                    .param("permission", "read")
                    .session(session)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz konnte nicht geteilt werden")))

            // Verify no share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 0
        }

        @Test
        fun `should redirect when note not found for sharing`() {
            mockMvc.perform(
                post("/notes/999/share")
                    .param("username", targetUser.username)
                    .param("permission", "read")
                    .session(session)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Neue Notiz erstellen")))
        }

        @Test
        fun `should fail when trying to share note already shared with user`() {
            // Create initial share
            noteShareRepository.save(
                NoteShare(
                    note = testNote,
                    sharedWithUser = targetUser,
                    sharedByUser = testUser,
                    permission = "read",
                    createdAt = LocalDateTime.now(),
                ),
            )

            // Try to share again
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", targetUser.username)
                    .param("permission", "read")
                    .session(session)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz konnte nicht geteilt werden")))

            // Verify only one share exists
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 1
        }

        @Test
        fun `should successfully revoke share`() {
            // Create a share first
            val noteShare = noteShareRepository.save(
                NoteShare(
                    note = testNote,
                    sharedWithUser = targetUser,
                    sharedByUser = testUser,
                    permission = "read",
                    createdAt = LocalDateTime.now(),
                ),
            )

            mockMvc.perform(
                delete("/notes/${testNote.id}/share/${targetUser.id}")
                    .session(session)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)

            // Verify share was deleted
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 0
        }

        @Test
        fun `should display notes shared with user`() {
            // Create a note owned by targetUser
            val sharedNote = noteRepository.save(
                Note(
                    title = "Shared Note",
                    content = "This note is shared with testuser",
                    user = targetUser,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Share the note with testUser
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser,
                    sharedByUser = targetUser,
                    permission = "read",
                    createdAt = LocalDateTime.now(),
                ),
            )

            mockMvc.perform(
                get("/notes/shared")
                    .session(session),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notes Shared With Me")))
                .andExpect(content().string(containsString("Shared Note")))
        }

        @Test
        fun `should show empty state when no shared notes`() {
            mockMvc.perform(
                get("/notes/shared")
                    .session(session),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("No shared notes found")))
        }

        @Test
        fun `should display both owned and shared notes`() {
            // Create a note owned by targetUser
            val sharedNote = noteRepository.save(
                Note(
                    title = "Shared Note",
                    content = "This note is shared with testuser",
                    user = targetUser,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Share the note with testUser
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser,
                    sharedByUser = targetUser,
                    permission = "read",
                    createdAt = LocalDateTime.now(),
                ),
            )

            mockMvc.perform(
                get("/notes/all")
                    .session(session),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("All Accessible Notes")))
                .andExpect(content().string(containsString("Test Note"))) // Owned note
                .andExpect(content().string(containsString("Shared Note"))) // Shared note
        }

        @Test
        fun `should successfully open edit modal for shared note`() {
            // Create a note owned by targetUser
            val sharedNote = noteRepository.save(
                Note(
                    title = "Shared Note for Edit",
                    content = "This note is shared with testuser for editing",
                    user = targetUser,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Share the note with testUser with write permission
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser,
                    sharedByUser = targetUser,
                    permission = "write",
                    createdAt = LocalDateTime.now(),
                ),
            )

            // Try to open edit modal for shared note - this should now work
            mockMvc.perform(
                get("/notes/modal/${sharedNote.id}/edit")
                    .session(session),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Shared Note for Edit")))
        }
    }
