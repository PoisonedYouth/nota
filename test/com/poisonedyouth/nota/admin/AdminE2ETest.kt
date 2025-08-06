package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.notes.Note
import com.poisonedyouth.nota.notes.NoteRepository
import com.poisonedyouth.nota.notes.NoteShare
import com.poisonedyouth.nota.notes.NoteShareRepository
import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminE2ETest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val noteRepository: NoteRepository,
        private val noteShareRepository: NoteShareRepository,
    ) {

        private lateinit var adminUsername: String
        private lateinit var user1Username: String
        private lateinit var user2Username: String

        private fun hashPassword(password: String): String {
            // Use BCrypt encoder consistent with production code
            val encoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            return encoder.encode(password)
        }

        @BeforeEach
        fun setup() {
            println("[DEBUG_LOG] Setting up admin E2E test data")

            // Clean up existing data
            noteShareRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()

            // Use unique usernames to avoid constraint violations
            val timestamp = System.currentTimeMillis()
            adminUsername = "testuser_admin_$timestamp"
            user1Username = "user1_$timestamp"
            user2Username = "user2_$timestamp"

            // Create admin user
            val adminUser = User(
                username = adminUsername,
                password = hashPassword("AdminPass123!"),
                role = UserRole.ADMIN,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 10, 0),
            )
            userRepository.save(adminUser)

            // Create regular users with different scenarios
            val user1 = User(
                username = user1Username,
                password = hashPassword("UserPassword1!"),
                mustChangePassword = true,
                role = UserRole.USER,
                createdAt = LocalDateTime.of(2024, 1, 2, 11, 0),
                updatedAt = LocalDateTime.of(2024, 1, 2, 11, 0),
            )
            val savedUser1 = userRepository.save(user1)

            val user2 = User(
                username = user2Username,
                password = hashPassword("UserPassword2!"),
                role = UserRole.USER,
                createdAt = LocalDateTime.of(2024, 1, 3, 12, 0),
                updatedAt = LocalDateTime.of(2024, 1, 3, 12, 0),
            )
            val savedUser2 = userRepository.save(user2)

            // Create notes for user1
            val note1 = Note(
                title = "User1 Note 1",
                content = "Content 1",
                user = savedUser1,
                createdAt = LocalDateTime.of(2024, 1, 4, 13, 0),
                updatedAt = LocalDateTime.of(2024, 1, 4, 13, 0),
            )
            val savedNote1 = noteRepository.save(note1)

            val note2 = Note(
                title = "User1 Note 2",
                content = "Content 2",
                archived = true,
                archivedAt = LocalDateTime.of(2024, 1, 5, 14, 0),
                user = savedUser1,
                createdAt = LocalDateTime.of(2024, 1, 4, 13, 30),
                updatedAt = LocalDateTime.of(2024, 1, 5, 14, 0),
            )
            val savedNote2 = noteRepository.save(note2)

            // Create notes for user2
            val note3 = Note(
                title = "User2 Note 1",
                content = "Content 3",
                user = savedUser2,
                createdAt = LocalDateTime.of(2024, 1, 6, 15, 0),
                updatedAt = LocalDateTime.of(2024, 1, 6, 15, 0),
            )
            val savedNote3 = noteRepository.save(note3)

            // Create note shares
            val noteShare1 = NoteShare(
                note = savedNote1,
                sharedWithUser = savedUser2,
                sharedByUser = savedUser1,
                permission = "read",
                createdAt = LocalDateTime.of(2024, 1, 7, 16, 0),
            )
            noteShareRepository.save(noteShare1)

            val noteShare2 = NoteShare(
                note = savedNote3,
                sharedWithUser = savedUser1,
                sharedByUser = savedUser2,
                permission = "read",
                createdAt = LocalDateTime.of(2024, 1, 8, 17, 0),
            )
            noteShareRepository.save(noteShare2)

            println("[DEBUG_LOG] Test data setup completed")
        }

        @Test
        fun `admin overview should display correct statistics and user data`() {
            println("[DEBUG_LOG] Testing admin overview functionality")

            // Login as admin
            val session = MockHttpSession()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .param("username", adminUsername)
                    .param("password", "AdminPass123!")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

            val adminUser = session.getAttribute("currentUser") as UserDto
            adminUser.username shouldBe adminUsername

            // Access admin overview
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("/admin/overview")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.view().name("admin/overview"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("userStatistics"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("systemStatistics"))
                .andReturn()

            val content = result.response.contentAsString
            println("[DEBUG_LOG] Admin overview HTML content length: ${content.length}")

            // Verify system statistics are displayed
            content shouldContain "System Statistiken"
            content shouldContain "3" // Total users (admin + user1 + user2)
            content shouldContain "Benutzer gesamt"
            content shouldContain "Notizen gesamt"
            content shouldContain "Archivierte Notizen"
            content shouldContain "Geteilte Notizen"

            // Verify user statistics table
            content shouldContain "Benutzer Statistiken"
            content shouldContain adminUsername
            content shouldContain "user1"
            content shouldContain "user2"
            content shouldContain "Passwort ändern erforderlich" // user1 has mustChangePassword = true
            content shouldContain "Aktiv" // admin and user2 have mustChangePassword = false

            // Verify table headers
            content shouldContain "Benutzername"
            content shouldContain "Registriert am"
            content shouldContain "Notizen gesamt"
            content shouldContain "Archivierte Notizen"
            content shouldContain "Geteilte Notizen"
            content shouldContain "Status"

            println("[DEBUG_LOG] Admin overview test completed successfully")
        }

        @Test
        fun `admin link should be visible only to admin users`() {
            println("[DEBUG_LOG] Testing admin link visibility")

            // Login as regular user
            val regularSession = MockHttpSession()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .param("username", user1Username)
                    .param("password", "UserPassword1!")
                    .session(regularSession),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

            // Check notes page - admin link should NOT be visible
            val regularResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/notes")
                    .session(regularSession),
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val regularContent = regularResult.response.contentAsString
            regularContent shouldContain "Meine Notizen"
            // Admin link should not be present for regular users
            // Note: The template uses th:if="${currentUser.role.name() == 'ADMIN'}"

            // Login as admin
            val adminSession = MockHttpSession()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .param("username", adminUsername)
                    .param("password", "AdminPass123!")
                    .session(adminSession),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

            // Check notes page - admin link should be visible
            val adminResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/notes")
                    .session(adminSession),
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val adminContent = adminResult.response.contentAsString
            adminContent shouldContain "Meine Notizen"
            adminContent shouldContain "Admin Übersicht"
            adminContent shouldContain "/admin/overview"

            println("[DEBUG_LOG] Admin link visibility test completed successfully")
        }

        @Test
        fun `non-admin users should not be able to access admin overview`() {
            println("[DEBUG_LOG] Testing admin access control")

            // Try to access admin overview without authentication
            mockMvc.perform(MockMvcRequestBuilders.get("/admin/overview"))
                .andExpect(MockMvcResultMatchers.status().isForbidden)

            // Login as regular user
            val session = MockHttpSession()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .param("username", user1Username)
                    .param("password", "UserPassword1!")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

            // Try to access admin overview as regular user
            mockMvc.perform(
                MockMvcRequestBuilders.get("/admin/overview")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
                .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))

            println("[DEBUG_LOG] Admin access control test completed successfully")
        }

        @Test
        fun `admin root path should redirect to overview`() {
            println("[DEBUG_LOG] Testing admin root redirect")

            // Login as admin
            val session = MockHttpSession()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .param("username", adminUsername)
                    .param("password", "AdminPass123!")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)

            // Access admin root path
            mockMvc.perform(
                MockMvcRequestBuilders.get("/admin")
                    .session(session),
            )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/overview"))

            println("[DEBUG_LOG] Admin root redirect test completed successfully")
        }
    }
