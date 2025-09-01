package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
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
        private val userService: com.poisonedyouth.nota.user.UserService,
    ) {
        private lateinit var ownerUser: User
        private lateinit var sharedUser: User
        private lateinit var testNote: Note
        private lateinit var ownerSession: MockHttpSession
        private lateinit var sharedSession: MockHttpSession

        @BeforeEach
        fun setup() {
            // Create a second test user using UserService to get proper password hashing
            val timestamp = System.currentTimeMillis()
            val sharedUsername = "sharetest_$timestamp"
            val registerDto = com.poisonedyouth.nota.user.RegisterDto(sharedUsername)
            val registrationResult = userService.registerUser(registerDto)
            sharedUser = userRepository.findByUsername(sharedUsername)!!
            
            // Update the user to not require password change for testing purposes
            val updatedSharedUser = User(
                id = sharedUser.id,
                username = sharedUser.username,
                password = sharedUser.password,
                mustChangePassword = false,
                role = sharedUser.role,
                createdAt = sharedUser.createdAt,
                updatedAt = LocalDateTime.now()
            )
            sharedUser = userRepository.save(updatedSharedUser)
            
            val testPassword = "TestPassword123!"
            val generatedPassword = registrationResult.initialPassword
            
            // Login as the existing test users - first login might not redirect if it's a first-time login
            ownerSession = MockHttpSession()
            mockMvc.perform(
                post("/auth/login")
                    .param("username", "testuser")
                    .param("password", testPassword)
                    .session(ownerSession)
            ) // Don't expect specific status for now
            
            sharedSession = MockHttpSession()
            mockMvc.perform(
                post("/auth/login")
                    .param("username", sharedUsername)
                    .param("password", generatedPassword)
                    .session(sharedSession)
            ) // Don't expect specific status for now
            
            
            
            
            // Get the owner user from database
            ownerUser = userRepository.findByUsername("testuser")!!

            // Create test note
            testNote =
                noteRepository.save(
                    Note(
                        title = "E2E Test Note",
                        content = "This note will be shared in E2E test",
                        user = ownerUser,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    ),
                )
        }

        private fun createAuthenticatedSession(user: User): MockHttpSession {
            val session = MockHttpSession()
            val userDto = UserDto(
                id = user.id!!,
                username = user.username,
                mustChangePassword = user.mustChangePassword,
                role = user.role
            )
            session.setAttribute("currentUser", userDto)
            return session
        }

        @AfterEach
        fun cleanup() {
            noteShareRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()
        }


        @Test
        @Transactional
        fun `Complete note sharing workflow`() {
            // Step 1: Owner shares note with another user
            mockMvc
                .perform(
                    post("/notes/${testNote.id}/share")
                        .param("username", sharedUser.username)
                        .param("permission", "read")
                        .header("HX-Request", "true")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("Note shared successfully!")))

            // Verify share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 1
            shares[0].sharedWithUser.username shouldBe sharedUser.username

            // Step 2: Shared user can see the note in their shared notes
            mockMvc
                .perform(
                    get("/notes/shared")
                        .session(createAuthenticatedSession(sharedUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("Notes shared with me")))

            // Step 3: Shared user can see the note in all accessible notes
            mockMvc
                .perform(
                    get("/notes/all")
                        .session(createAuthenticatedSession(sharedUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("All accessible notes")))

            // Step 4: Shared user can view the note details
            mockMvc
                .perform(
                    get("/notes/${testNote.id}")
                        .session(createAuthenticatedSession(sharedUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("E2E Test Note")))
                .andExpect(content().string(containsString("This note will be shared in E2E test")))

            // Step 5: Owner can see who the note is shared with
            mockMvc
                .perform(
                    get("/notes/modal/${testNote.id}")
                        .param("mode", "share")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("Currently shared with:")))
                .andExpect(content().string(containsString(sharedUser.username)))

            // Step 6: Owner revokes the share
            mockMvc
                .perform(
                    delete("/notes/${testNote.id}/share/${sharedUser.id}")
                        .header("HX-Request", "true")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)

            // Verify share was deleted
            val sharesAfterRevoke = noteShareRepository.findAllByNote(testNote)
            sharesAfterRevoke.size shouldBe 0

            // Step 7: Shared user can no longer see the note
            mockMvc
                .perform(
                    get("/notes/shared")
                        .session(createAuthenticatedSession(sharedUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("No shared notes found")))

            // Step 8: Shared user cannot access note details directly
            mockMvc
                .perform(
                    get("/notes/${testNote.id}")
                        .session(createAuthenticatedSession(sharedUser)),
                ).andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("/notes"))
        }

        @Test
        @Transactional
        fun `Share note with multiple users`() {

            // Create another user
            val timestamp = System.currentTimeMillis()
            val anotherUser =
                userRepository.save(
                    User(
                        username = "anotheruser_e2e_$timestamp",
                        password = "password",
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    ),
                )

            // Share with first user
            mockMvc
                .perform(
                    post("/notes/${testNote.id}/share")
                        .param("username", sharedUser.username)
                        .param("permission", "read")
                        .header("HX-Request", "true")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)

            // Share with second user
            mockMvc
                .perform(
                    post("/notes/${testNote.id}/share")
                        .param("username", anotherUser.username)
                        .param("permission", "read")
                        .header("HX-Request", "true")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)

            // Verify both shares exist
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 2

            val usernames = shares.map { it.sharedWithUser.username }.toSet()
            usernames shouldBe setOf(sharedUser.username, anotherUser.username)
        }

        @Test
        fun `Prevent sharing with non-existent user`() {
            mockMvc
                .perform(
                    post("/notes/${testNote.id}/share")
                        .param("username", "nonexistentuser")
                        .param("permission", "read")
                        .header("HX-Request", "true")
                        .session(createAuthenticatedSession(ownerUser)),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString("Note could not be shared")))

            // Verify no share was created
            val shares = noteShareRepository.findAllByNote(testNote)
            shares.size shouldBe 0
        }
    }
