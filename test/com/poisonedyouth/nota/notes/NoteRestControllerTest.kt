package com.poisonedyouth.nota.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class NoteRestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var noteService: NoteService
    private lateinit var activityEventPublisher: ActivityEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        noteService = mockk()
        activityEventPublisher = mockk()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        val controller = NoteRestController(noteService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun createMockSession(): MockHttpSession {
        val session = MockHttpSession()
        val user =
            UserDto(
                id = 1L,
                username = "testuser",
                mustChangePassword = false,
                role = UserRole.USER,
            )
        session.setAttribute("currentUser", user)
        return session
    }

    private fun createMockNote(): NoteDto =
        NoteDto(
            id = 1L,
            title = "Test Note",
            content = "<p>Test content</p>",
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now(),
            archived = false,
            archivedAt = null,
            dueDate = LocalDateTime.now().plusDays(1),
            userId = 1L,
            user = UserDto(1L, "testuser", false, UserRole.USER),
        )

    @Test
    fun `should create note successfully`() {
        // Given
        val session = createMockSession()
        val createNoteDto = CreateNoteDto("New Note", "<p>New content</p>")
        val mockNote = createMockNote()

        every { noteService.createNote(createNoteDto, 1L) } returns mockNote
        every { activityEventPublisher.publishCreateNoteEvent(1L, 1L, "Test Note") } just runs

        // When & Then
        mockMvc
            .perform(
                post("/api/notes")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createNoteDto)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Test Note"))
            .andExpect(jsonPath("$.content").value("<p>Test content</p>"))
            .andExpect(jsonPath("$.userId").value(1L))

        verify { noteService.createNote(createNoteDto, 1L) }
        verify { activityEventPublisher.publishCreateNoteEvent(1L, 1L, "Test Note") }
    }

    @Test
    fun `should return unauthorized when not authenticated for create note`() {
        // Given
        val createNoteDto = CreateNoteDto("New Note", "<p>New content</p>")

        // When & Then
        mockMvc
            .perform(
                post("/api/notes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createNoteDto)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Authentication required"))
    }

    @Test
    fun `should return bad request when note content is empty`() {
        // Given
        val session = createMockSession()
        val createNoteDto = CreateNoteDto("New Note", "")

        every { noteService.createNote(createNoteDto, 1L) } throws IllegalArgumentException("Note content cannot be empty")

        // When & Then
        mockMvc
            .perform(
                post("/api/notes")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createNoteDto)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Note content cannot be empty"))
    }

    @Test
    fun `should list notes successfully`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.findAllNotes(1L, "updatedAt", "desc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.notes[0].id").value(1L))
            .andExpect(jsonPath("$.notes[0].title").value("Test Note"))
            .andExpect(jsonPath("$.count").value(1))
    }

    @Test
    fun `should list notes with custom sorting`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.findAllNotes(1L, "title", "asc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes")
                    .session(session)
                    .param("sort", "title")
                    .param("order", "asc"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.count").value(1))

        verify { noteService.findAllNotes(1L, "title", "asc") }
    }

    @Test
    fun `should get note by id successfully`() {
        // Given
        val session = createMockSession()
        val mockNote = createMockNote()

        every { noteService.findAccessibleNoteById(1L, 1L) } returns mockNote

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/1")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Test Note"))
    }

    @Test
    fun `should return not found when note does not exist`() {
        // Given
        val session = createMockSession()

        every { noteService.findAccessibleNoteById(999L, 1L) } returns null

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/999")
                    .session(session),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should update note successfully`() {
        // Given
        val session = createMockSession()
        val updateNoteDto = UpdateNoteDto(1L, "Updated Note", "<p>Updated content</p>")
        val updatedNote = createMockNote().copy(title = "Updated Note", content = "<p>Updated content</p>")

        every { noteService.updateNote(updateNoteDto, 1L) } returns updatedNote
        every { activityEventPublisher.publishUpdateNoteEvent(1L, 1L, "Updated Note") } just runs

        // When & Then
        mockMvc
            .perform(
                put("/api/notes/1")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateNoteDto)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated Note"))
            .andExpect(jsonPath("$.content").value("<p>Updated content</p>"))

        verify { noteService.updateNote(updateNoteDto, 1L) }
        verify { activityEventPublisher.publishUpdateNoteEvent(1L, 1L, "Updated Note") }
    }

    @Test
    fun `should return not found when updating non-existent note`() {
        // Given
        val session = createMockSession()
        val updateNoteDto = UpdateNoteDto(999L, "Updated Note", "<p>Updated content</p>")

        every { noteService.updateNote(updateNoteDto, 1L) } returns null

        // When & Then
        mockMvc
            .perform(
                put("/api/notes/999")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateNoteDto)),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should archive note successfully`() {
        // Given
        val session = createMockSession()
        val mockNote = createMockNote()

        every { noteService.findAccessibleNoteById(1L, 1L) } returns mockNote
        every { noteService.archiveNote(1L, 1L) } returns true
        every { activityEventPublisher.publishArchiveNoteEvent(1L, 1L, "Test Note") } just runs

        // When & Then
        mockMvc
            .perform(
                delete("/api/notes/1")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Note archived successfully"))

        verify { noteService.archiveNote(1L, 1L) }
        verify { activityEventPublisher.publishArchiveNoteEvent(1L, 1L, "Test Note") }
    }

    @Test
    fun `should return not found when archiving non-existent note`() {
        // Given
        val session = createMockSession()

        every { noteService.findAccessibleNoteById(999L, 1L) } returns null
        every { noteService.archiveNote(999L, 1L) } returns false

        // When & Then
        mockMvc
            .perform(
                delete("/api/notes/999")
                    .session(session),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should search notes successfully`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.searchNotes("test", 1L, "updatedAt", "desc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/search")
                    .session(session)
                    .param("q", "test"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.notes[0].title").value("Test Note"))
            .andExpect(jsonPath("$.query").value("test"))
            .andExpect(jsonPath("$.count").value(1))
    }

    @Test
    fun `should search accessible notes when all parameter is true`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.searchAccessibleNotes("test", 1L, "updatedAt", "desc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/search")
                    .session(session)
                    .param("q", "test")
                    .param("all", "true"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.count").value(1))

        verify { noteService.searchAccessibleNotes("test", 1L, "updatedAt", "desc") }
    }

    @Test
    fun `should list shared notes successfully`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.findAllSharedNotes(1L, "updatedAt", "desc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/shared")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.count").value(1))
    }

    @Test
    fun `should list all accessible notes successfully`() {
        // Given
        val session = createMockSession()
        val mockNotes = listOf(createMockNote())

        every { noteService.findAllAccessibleNotes(1L, "updatedAt", "desc") } returns mockNotes

        // When & Then
        mockMvc
            .perform(
                get("/api/notes/all")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.count").value(1))
    }

    @Test
    fun `should return bad request when updating note with blank title`() {
        // Given
        val session = createMockSession()
        val updateNoteDto = UpdateNoteDto(1L, "", "<p>Updated content</p>")

        // When & Then
        mockMvc
            .perform(
                put("/api/notes/1")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateNoteDto)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return bad request when updating note with blank content`() {
        // Given
        val session = createMockSession()
        val updateNoteDto = UpdateNoteDto(1L, "Valid Title", "")

        // When & Then
        mockMvc
            .perform(
                put("/api/notes/1")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateNoteDto)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return bad request when creating note with blank title`() {
        // Given
        val session = createMockSession()
        val createNoteDto = CreateNoteDto("", "<p>Valid content</p>")

        // When & Then
        mockMvc
            .perform(
                post("/api/notes")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createNoteDto)),
            ).andExpect(status().isBadRequest)
    }
}
