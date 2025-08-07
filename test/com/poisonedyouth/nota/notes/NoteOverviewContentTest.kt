package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoteOverviewContentTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository,
    ) {
        private lateinit var testUser: User
        private lateinit var testSession: MockHttpSession

        @BeforeEach
        fun setup() {
            noteRepository.deleteAll()
            userRepository.deleteAll()

            // Create test user
            testUser =
                userRepository.save(
                    User(
                        username = "testuser_overview_${System.currentTimeMillis()}",
                        password = "password",
                    ),
                )

            // Create session with authentication
            testSession = MockHttpSession()
            testSession.setAttribute(
                "currentUser",
                UserDto(
                    id = testUser.id!!,
                    username = testUser.username,
                    mustChangePassword = testUser.mustChangePassword,
                    role = UserRole.USER,
                ),
            )

            // Create test notes
            val now = LocalDateTime.now()
            val note1 =
                Note(
                    title = "Test Note 1",
                    content = "This is test content for note 1 that should be visible in preview",
                    createdAt = now,
                    updatedAt = now,
                    user = testUser,
                )
            val note2 =
                Note(
                    title = "Test Note 2",
                    content = "This is test content for note 2 that should be visible in preview",
                    createdAt = now,
                    updatedAt = now.plusMinutes(1),
                    user = testUser,
                )
            noteRepository.saveAll(listOf(note1, note2))
        }

        @Test
        fun `note overview should only show titles after fix`() {
            // When - accessing the notes list page
            val result =
                mockMvc
                    .perform(
                        get("/notes")
                            .session(testSession),
                    ).andExpect(status().isOk)
                    .andReturn()

            val content = result.response.contentAsString

            // Then - only titles should be visible, no content preview
            content shouldContain "note-title"
            content shouldNotContain "note-content"
            content shouldNotContain "This is test content"
        }

        @Test
        fun `all notes overview should only show titles after fix`() {
            // When - accessing the all notes page
            val result =
                mockMvc
                    .perform(
                        get("/notes/all")
                            .session(testSession),
                    ).andExpect(status().isOk)
                    .andReturn()

            val content = result.response.contentAsString

            // Then - only titles should be visible, no content preview
            content shouldContain "note-title"
            content shouldNotContain "note-content"
            content shouldNotContain "This is test content"
        }

        @Test
        fun `shared notes overview should only show titles after fix`() {
            // When - accessing the shared notes page
            val result =
                mockMvc
                    .perform(
                        get("/notes/shared")
                            .session(testSession),
                    ).andExpect(status().isOk)
                    .andReturn()

            val content = result.response.contentAsString

            // Then - page should load correctly and not contain content-related elements
            content shouldContain "Mit mir geteilte Notizen"
            content shouldNotContain "note-content"
            content shouldNotContain "getContentPreview"
        }
    }
