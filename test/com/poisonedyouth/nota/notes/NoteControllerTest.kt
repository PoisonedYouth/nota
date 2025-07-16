package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserDto
import io.mockk.every
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
    private lateinit var mockSession: MockHttpSession
    private lateinit var testUser: UserDto

    @BeforeEach
    fun setup() {
        noteService = mockk()
        mockSession = MockHttpSession()
        testUser = UserDto(
            id = 1L,
            username = "testuser",
            mustChangePassword = false,
        )

        mockSession.setAttribute("currentUser", testUser)

        val controller = NoteController(noteService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `listNotes should return list view with notes`() {
        // Given
        val now = LocalDateTime.now()
        val notes = listOf(
            NoteDto(
                id = 1L,
                title = "Note 1",
                content = "Content 1",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
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
            ),
        )
        every { noteService.findAllNotes(1L) } returns notes

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes").session(mockSession))
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
        mockMvc.perform(MockMvcRequestBuilders.get("/notes").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", emptyList<NoteDto>()))
    }

    @Test
    fun `archiveNote should return redirect for regular request`() {
        // Given
        val noteId = 1L
        every { noteService.archiveNote(noteId, 1L) } returns true

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { noteService.archiveNote(noteId, 1L) }
    }

    @Test
    fun `archiveNote should return archive response fragment for HTMX request`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val remainingNotes = listOf(
            NoteDto(
                id = 2L,
                title = "Note 2",
                content = "Content 2",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
            ),
        )
        every { noteService.archiveNote(noteId, 1L) } returns true
        every { noteService.findAllNotes(1L) } returns remainingNotes

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/notes/$noteId")
                .header("HX-Request", "true")
                .session(mockSession),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
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
        val searchResults = listOf(
            NoteDto(
                id = 1L,
                title = "Test Note",
                content = "Test content",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
            ),
        )
        every { noteService.searchNotes(query, 1L) } returns searchResults

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/search").param("q", query).session(mockSession))
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
        val searchResults = listOf(
            NoteDto(
                id = 2L,
                title = "Important Note",
                content = "Important content",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
            ),
        )
        every { noteService.searchNotes(query, 1L) } returns searchResults

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/notes/search")
                .param("q", query)
                .header("HX-Request", "true")
                .session(mockSession),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
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
        val allNotes = listOf(
            NoteDto(
                id = 1L,
                title = "Note 1",
                content = "Content 1",
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                dueDate = null,
            ),
        )
        every { noteService.searchNotes("", 1L) } returns allNotes

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/search").session(mockSession))
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
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/search").param("q", query).session(mockSession))
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
        val note = NoteDto(
            id = noteId,
            title = "Test Note",
            content = "This is the full content of the test note",
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            dueDate = now.plusDays(1),
        )
        every { noteService.findNoteById(noteId, 1L) } returns note

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/detail"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("note"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("currentUser"))
            .andExpect(MockMvcResultMatchers.model().attribute("note", note))

        verify { noteService.findNoteById(noteId, 1L) }
    }

    @Test
    fun `showNoteDetail should redirect when note not found`() {
        // Given
        val noteId = 999L
        every { noteService.findNoteById(noteId, 1L) } returns null

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/$noteId").session(mockSession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { noteService.findNoteById(noteId, 1L) }
    }

    @Test
    fun `showNoteDetail should redirect when user not authenticated`() {
        // Given
        val noteId = 1L
        val emptySession = MockHttpSession()

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes/$noteId").session(emptySession))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }
}
