package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.collections.shouldContain
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoteE2ETest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val noteRepository: NoteRepository,
        private val noteService: NoteService,
        private val userRepository: UserRepository,
    ) {

        private lateinit var testUser: User
        private lateinit var testSession: MockHttpSession

        @BeforeEach
        fun setup() {
            noteRepository.deleteAll()
            userRepository.deleteAll()

            // Use a unique username for each test run to avoid conflicts
            val uniqueUsername = "testuser_e2e_${System.currentTimeMillis()}"
            testUser = userRepository.save(
                User(
                    username = uniqueUsername,
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
        }

        @Test
        fun `should return 200 OK when accessing notes endpoint`() {
            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
        }

        @Test
        fun `should create and retrieve notes correctly`() {
            // Given
            val now = LocalDateTime.now()
            val note1 = Note(title = "E2E Test Note 1", content = "E2E Test Content 1", createdAt = now, updatedAt = now, user = testUser)
            val note2 =
                Note(
                    title = "E2E Test Note 2",
                    content = "E2E Test Content 2",
                    createdAt = now,
                    updatedAt = now.plusHours(1),
                    user = testUser,
                )
            noteRepository.saveAll(listOf(note1, note2))

            // When
            val notes = noteService.findAllNotes(testUser.id!!)

            // Then
            notes.size shouldBe 2

            // Verify the notes have the expected content
            val titles = notes.map { it.title }
            titles shouldContain "E2E Test Note 1"
            titles shouldContain "E2E Test Note 2"

            // Verify the notes are ordered correctly (most recent first)
            notes[0].title shouldBe "E2E Test Note 2"
            notes[1].title shouldBe "E2E Test Note 1"

            // Verify the DTO methods work correctly
            val firstNote = notes[0]
            firstNote.getContentPreview() shouldContain "E2E Test Content 2"
            firstNote.getFormattedDate() shouldContain now.plusHours(1).year.toString()
        }

        @Test
        fun `should display owner information in note list view`() {
            // Given
            val note = Note(title = "Test Note", content = "Test Content", user = testUser)
            noteRepository.save(note)

            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ersteller: ${testUser.username}")))
        }

        @Test
        fun `should display owner information in note detail view`() {
            // Given
            val note = Note(title = "Test Note", content = "Test Content", user = testUser)
            val savedNote = noteRepository.save(note)

            // When
            val result = mockMvc.perform(
                get("/notes/${savedNote.id}")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ersteller:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(testUser.username)))
        }

        @Test
        fun `should apply overdue CSS class to overdue notes in HTML`() {
            // Given
            val pastDueDate = LocalDateTime.now().minusHours(2)
            val overdueNote = Note(
                title = "Overdue Note",
                content = "This note is overdue",
                dueDate = pastDueDate,
                user = testUser,
            )
            noteRepository.save(overdueNote)

            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("note-overdue")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Overdue Note")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fällig:")))
        }

        @Test
        fun `should apply due-soon CSS class to notes due within 24 hours in HTML`() {
            // Given
            val dueSoonDate = LocalDateTime.now().plusHours(12)
            val dueSoonNote = Note(
                title = "Due Soon Note",
                content = "This note is due soon",
                dueDate = dueSoonDate,
                user = testUser,
            )
            noteRepository.save(dueSoonNote)

            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("note-due-soon")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Due Soon Note")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fällig:")))
        }

        @Test
        fun `should not apply special CSS classes to notes with future due dates in HTML`() {
            // Given
            val futureDueDate = LocalDateTime.now().plusDays(2)
            val futureNote = Note(
                title = "Future Note",
                content = "This note is due in the future",
                dueDate = futureDueDate,
                user = testUser,
            )
            noteRepository.save(futureNote)

            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Future Note")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fällig:")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("note-overdue"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("note-due-soon"))))
        }

        @Test
        fun `should not apply special CSS classes to notes without due dates in HTML`() {
            // Given
            val noteWithoutDueDate = Note(
                title = "No Due Date Note",
                content = "This note has no due date",
                dueDate = null,
                user = testUser,
            )
            noteRepository.save(noteWithoutDueDate)

            // When
            val result = mockMvc.perform(
                get("/notes")
                    .session(testSession),
            )

            // Then
            result.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No Due Date Note")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("note-overdue"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("note-due-soon"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Fällig:"))))
        }

        @Test
        fun `should search notes and return results in HTML`() {
            // Given
            val searchableNote = Note(
                title = "Important Meeting",
                content = "Discuss project timeline and deliverables",
                user = testUser,
            )
            val anotherNote = Note(
                title = "Shopping List",
                content = "Buy groceries for the important dinner party",
                user = testUser,
            )
            val unrelatedNote = Note(
                title = "Random Note",
                content = "Some random content here",
                user = testUser,
            )

            noteRepository.save(searchableNote)
            noteRepository.save(anotherNote)
            noteRepository.save(unrelatedNote)

            // When - search for "important"
            val searchResult = mockMvc.perform(
                get("/notes/search")
                    .param("q", "important")
                    .session(testSession),
            )

            // Then
            searchResult.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Important Meeting")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Shopping List")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Random Note"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("search-input"))) // Search form should be present
        }

        @Test
        fun `should return empty search results when no matches found`() {
            // Given
            val note = Note(
                title = "Test Note",
                content = "Test content",
                user = testUser,
            )
            noteRepository.save(note)

            // When - search for non-existent term
            val searchResult = mockMvc.perform(
                get("/notes/search")
                    .param("q", "nonexistent")
                    .session(testSession),
            )

            // Then
            searchResult.andExpect(status().isOk)
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Test Note"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("search-input"))) // Search form should still be present
        }
    }
