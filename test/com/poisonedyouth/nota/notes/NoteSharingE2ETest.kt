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
class NoteSharingE2ETest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val noteRepository: NoteRepository,
        private val noteShareRepository: NoteShareRepository,
    ) {

        private lateinit var ownerUser: User
        private lateinit var sharedUser: User
        private lateinit var testNote: Note
        private lateinit var ownerSession: MockHttpSession
        private lateinit var sharedSession: MockHttpSession

        @BeforeEach
        fun setup() {
            // Create test users with unique usernames to avoid conflicts
            val timestamp = System.currentTimeMillis()
            ownerUser = userRepository.save(
                User(
                    username = "noteowner_e2e_$timestamp",
                    password = "password",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            sharedUser = userRepository.save(
                User(
                    username = "shareduser_e2e_$timestamp",
                    password = "password",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Create test note
            testNote = noteRepository.save(
                Note(
                    title = "E2E Test Note",
                    content = "This note will be shared in E2E test",
                    user = ownerUser,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Create sessions for both users
            ownerSession = MockHttpSession()
            ownerSession.setAttribute(
                "currentUser",
                UserDto(
                    id = ownerUser.id!!,
                    username = ownerUser.username,
                    mustChangePassword = ownerUser.mustChangePassword,
                ),
            )

            sharedSession = MockHttpSession()
            sharedSession.setAttribute(
                "currentUser",
                UserDto(
                    id = sharedUser.id!!,
                    username = sharedUser.username,
                    mustChangePassword = sharedUser.mustChangePassword,
                ),
            )
        }

        @AfterEach
        fun cleanup() {
            noteShareRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()
        }

        @Test
        fun `Complete note sharing workflow`() {
            println("[DEBUG_LOG] Starting complete note sharing workflow test")

            // Step 1: Owner shares note with another user
            println("[DEBUG_LOG] Step 1: Sharing note with user '${sharedUser.username}'")
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", sharedUser.username)
                    .param("permission", "read")
                    .session(ownerSession)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz erfolgreich geteilt!")))

            // Verify share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 1
            shares[0].sharedWithUser.username shouldBe sharedUser.username
            println("[DEBUG_LOG] Share created successfully")

            // Step 2: Shared user can see the note in their shared notes
            println("[DEBUG_LOG] Step 2: Checking shared user can see the note")
            mockMvc.perform(
                get("/notes/shared")
                    .session(sharedSession),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("Mit mir geteilte Notizen")))

            // Step 3: Shared user can see the note in all accessible notes
            println("[DEBUG_LOG] Step 3: Checking note appears in all accessible notes")
            mockMvc.perform(
                get("/notes/all")
                    .session(sharedSession),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("Alle zugänglichen Notizen")))

            // Step 4: Shared user can view the note details
            println("[DEBUG_LOG] Step 4: Checking shared user can view note details")
            mockMvc.perform(
                get("/notes/${testNote.id}")
                    .session(sharedSession),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("This note will be shared in E2E test")))

            // Step 5: Owner can see who the note is shared with
            println("[DEBUG_LOG] Step 5: Checking owner can see share information")
            mockMvc.perform(
                get("/notes/modal/${testNote.id}")
                    .param("mode", "share")
                    .session(ownerSession),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz teilen: E2E Test Note")))
                .andExpect(content().string(containsString(sharedUser.username)))

            // Step 6: Owner revokes the share
            println("[DEBUG_LOG] Step 6: Revoking share")
            mockMvc.perform(
                delete("/notes/${testNote.id}/share/${sharedUser.id}")
                    .session(ownerSession)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)

            // Verify share was deleted
            val sharesAfterRevoke = noteShareRepository.findAllByNote(testNote)
            sharesAfterRevoke.size shouldBe 0
            println("[DEBUG_LOG] Share revoked successfully")

            // Step 7: Shared user can no longer see the note
            println("[DEBUG_LOG] Step 7: Verifying shared user can no longer access note")
            mockMvc.perform(
                get("/notes/shared")
                    .session(sharedSession),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Keine geteilten Notizen gefunden")))

            // Step 8: Shared user cannot access note details directly
            println("[DEBUG_LOG] Step 8: Verifying direct access is denied")
            mockMvc.perform(
                get("/notes/${testNote.id}")
                    .session(sharedSession),
            )
                .andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("/notes"))

            println("[DEBUG_LOG] Complete note sharing workflow test completed successfully")
        }

        @Test
        fun `Share note with multiple users`() {
            println("[DEBUG_LOG] Starting multiple users sharing test")

            // Create another user
            val timestamp = System.currentTimeMillis()
            val anotherUser = userRepository.save(
                User(
                    username = "anotheruser_e2e_$timestamp",
                    password = "password",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

            // Share with first user
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", sharedUser.username)
                    .param("permission", "read")
                    .session(ownerSession)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)

            // Share with second user
            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", anotherUser.username)
                    .param("permission", "read")
                    .session(ownerSession)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)

            // Verify both shares exist
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 2

            val usernames = shares.map { it.sharedWithUser.username }.toSet()
            usernames shouldBe setOf(sharedUser.username, anotherUser.username)

            println("[DEBUG_LOG] Multiple users sharing test completed successfully")
        }

        @Test
        fun `Prevent sharing with non-existent user`() {
            println("[DEBUG_LOG] Starting non-existent user sharing test")

            mockMvc.perform(
                post("/notes/${testNote.id}/share")
                    .param("username", "nonexistentuser")
                    .param("permission", "read")
                    .session(ownerSession)
                    .header("HX-Request", "true"),
            )
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Notiz konnte nicht geteilt werden")))

            // Verify no share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 0

            println("[DEBUG_LOG] Non-existent user sharing test completed successfully")
        }
    }
