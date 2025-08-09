package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.activitylog.ActivityLogService
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class NoteControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var noteService: NoteService
    private lateinit var activityLogService: ActivityLogService
    private lateinit var activityEventPublisher: ActivityEventPublisher
    private lateinit var noteAttachmentService: NoteAttachmentService
    private lateinit var mockSession: MockHttpSession
    private lateinit var testUser: UserDto

    @BeforeEach
    fun setup() {
        noteService = mockk()
        activityLogService = mockk()
        activityEventPublisher = mockk()
        noteAttachmentService = mockk()
        mockSession = MockHttpSession()
        testUser =
            UserDto(
                id = 1L,
                username = "testuser",
                mustChangePassword = false,
                role = UserRole.USER,
            )

        mockSession.setAttribute("currentUser", testUser)

        val controller = NoteController(noteService, activityLogService, activityEventPublisher, noteAttachmentService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `listNotes should return list view with notes`() {
        // Given
        val now = LocalDateTime.now()
        val notes =
            listOf(
                NoteDto(
                    id = 1L,
                    title = "Note 1",
                    content = "Content 1",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
                NoteDto(
                    id = 2L,
                    title = "Note 2",
                    content = "Content 2",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
            )
        every { noteService.findAllNotes(1L) } returns notes

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", notes))
    }

    @Test
    fun `listNotes should return list view with empty list when no notes exist`() {
        // Given
        every { noteService.findAllNotes(1L) } returns emptyList()

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", emptyList<NoteDto>()))
    }

    @Test
    fun `archiveNote should return redirect for regular request`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val noteDto =
            NoteDto(
                id = noteId,
                title = "Test Note",
                content = "Test content",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
                userId = 1L,
                user = testUser,
            )
        every { noteService.findAccessibleNoteById(noteId, 1L) } returns noteDto
        every { noteService.archiveNote(noteId, 1L) } returns true
        every { activityEventPublisher.publishArchiveNoteEvent(any(), any(), any()) } just Runs

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.delete("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { noteService.archiveNote(noteId, 1L) }
    }

    @Test
    fun `archiveNote should return archive response fragment for HTMX request`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val noteDto =
            NoteDto(
                id = noteId,
                title = "Test Note",
                content = "Test content",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
                userId = 1L,
                user = testUser,
            )
        val remainingNotes =
            listOf(
                NoteDto(
                    id = 2L,
                    title = "Note 2",
                    content = "Content 2",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
            )
        every { noteService.findAccessibleNoteById(noteId, 1L) } returns noteDto
        every { noteService.archiveNote(noteId, 1L) } returns true
        every { noteService.findAllNotes(1L) } returns remainingNotes
        every { activityEventPublisher.publishArchiveNoteEvent(any(), any(), any()) } just Runs

        // When/Then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .delete("/notes/$noteId")
                    .header("HX-Request", "true")
                    .session(mockSession),
            ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/fragments :: archive-response"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", remainingNotes))

        verify { noteService.archiveNote(noteId, 1L) }
        verify { noteService.findAllNotes(1L) }
    }

    @Test
    fun `searchNotes should return list view with search results for regular request`() {
        // Given
        val query = "test"
        val now = LocalDateTime.now()
        val searchResults =
            listOf(
                NoteDto(
                    id = 1L,
                    title = "Test Note",
                    content = "Test content",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
            )
        every { noteService.searchNotes(query, 1L) } returns searchResults

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/search").param("q", query).session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("searchQuery"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", searchResults))
            .andExpect(MockMvcResultMatchers.model().attribute("searchQuery", query))

        verify { noteService.searchNotes(query, 1L) }
    }

    @Test
    fun `searchNotes should return notes container fragment for HTMX request`() {
        // Given
        val query = "important"
        val now = LocalDateTime.now()
        val searchResults =
            listOf(
                NoteDto(
                    id = 2L,
                    title = "Important Note",
                    content = "Important content",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
            )
        every { noteService.searchNotes(query, 1L) } returns searchResults

        // When/Then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/notes/search")
                    .param("q", query)
                    .header("HX-Request", "true")
                    .session(mockSession),
            ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list :: #notes-container"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("searchQuery"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", searchResults))
            .andExpect(MockMvcResultMatchers.model().attribute("searchQuery", query))

        verify { noteService.searchNotes(query, 1L) }
    }

    @Test
    fun `searchNotes should handle empty query parameter`() {
        // Given
        val now = LocalDateTime.now()
        val allNotes =
            listOf(
                NoteDto(
                    id = 1L,
                    title = "Note 1",
                    content = "Content 1",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
            )
        every { noteService.searchNotes("", 1L) } returns allNotes

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/search").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("searchQuery"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", allNotes))
            .andExpect(MockMvcResultMatchers.model().attribute("searchQuery", ""))

        verify { noteService.searchNotes("", 1L) }
    }

    @Test
    fun `searchNotes should return empty results when no matches found`() {
        // Given
        val query = "nonexistent"
        every { noteService.searchNotes(query, 1L) } returns emptyList()

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/search").param("q", query).session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("searchQuery"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", emptyList<NoteDto>()))
            .andExpect(MockMvcResultMatchers.model().attribute("searchQuery", query))

        verify { noteService.searchNotes(query, 1L) }
    }

    @Test
    fun `showNoteDetail should return detail view when note exists`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val note =
            NoteDto(
                id = noteId,
                title = "Test Note",
                content = "This is the full content of the test note",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = now.plusDays(1),
                userId = 1L,
                user = testUser,
            )
        every { noteService.findAccessibleNoteById(noteId, 1L) } returns note
        every { noteAttachmentService.listAttachments(noteId, 1L) } returns emptyList()

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/detail"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("note"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("attachments"))
            .andExpect(MockMvcResultMatchers.model().attribute("note", note))

        verify { noteService.findAccessibleNoteById(noteId, 1L) }
        verify { noteAttachmentService.listAttachments(noteId, 1L) }
    }

    @Test
    fun `showNoteDetail should redirect when note not found`() {
        // Given
        val noteId = 999L
        every { noteService.findAccessibleNoteById(noteId, 1L) } returns null

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { noteService.findAccessibleNoteById(noteId, 1L) }
    }

    @Test
    fun `showNoteDetail should redirect when user not authenticated`() {
        // Given
        val noteId = 1L
        val emptySession = MockHttpSession()

        // When/Then
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes/$noteId").session(emptySession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `searchNotes should use searchAccessibleNotes when all parameter is true`() {
        // Given
        val query = "test"
        val now = LocalDateTime.now()
        val accessibleNotes =
            listOf(
                NoteDto(
                    id = 1L,
                    title = "My Test Note",
                    content = "My test content",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 1L,
                    user = testUser,
                ),
                NoteDto(
                    id = 2L,
                    title = "Shared Test Note",
                    content = "Shared test content",
                    createdAt = now,
                    updatedAt = now,
                    archived = false,
                    archivedAt = null,
                    dueDate = null,
                    userId = 2L,
                    user = UserDto(2L, "otheruser", false, UserRole.USER),
                ),
            )
        every { noteService.searchAccessibleNotes(query, 1L) } returns accessibleNotes

        // When/Then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/notes/search")
                    .param("q", query)
                    .param("all", "true")
                    .header("HX-Request", "true")
                    .session(mockSession),
            ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/all :: #notes-container"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("searchQuery"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", accessibleNotes))
            .andExpect(MockMvcResultMatchers.model().attribute("searchQuery", query))

        verify { noteService.searchAccessibleNotes(query, 1L) }
    }
}
