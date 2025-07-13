package com.poisonedyouth.nota.notes

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class NoteControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var noteService: NoteService

    @BeforeEach
    fun setup() {
        noteService = mockk()
        val controller = NoteController(noteService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `listNotes should return list view with notes`() {
        // Given
        val now = LocalDateTime.now()
        val notes = listOf(
            NoteDto(id = 1L, title = "Note 1", content = "Content 1", createdAt = now, updatedAt = now, archived = false, archivedAt = null, dueDate = null),
            NoteDto(id = 2L, title = "Note 2", content = "Content 2", createdAt = now, updatedAt = now, archived = false, archivedAt = null, dueDate = null),
        )
        every { noteService.findAllNotes() } returns notes

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", notes))
    }

    @Test
    fun `listNotes should return list view with empty list when no notes exist`() {
        // Given
        every { noteService.findAllNotes() } returns emptyList()

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/notes"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", emptyList<NoteDto>()))
    }

    @Test
    fun `archiveNote should return redirect for regular request`() {
        // Given
        val noteId = 1L
        every { noteService.archiveNote(noteId) } returns true

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/notes/$noteId"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { noteService.archiveNote(noteId) }
    }

    @Test
    fun `archiveNote should return archive response fragment for HTMX request`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val remainingNotes = listOf(
            NoteDto(id = 2L, title = "Note 2", content = "Content 2", createdAt = now, updatedAt = now, archived = false, archivedAt = null, dueDate = null),
        )
        every { noteService.archiveNote(noteId) } returns true
        every { noteService.findAllNotes() } returns remainingNotes

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/notes/$noteId")
                .header("HX-Request", "true"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("notes/fragments :: archive-response"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("notes"))
            .andExpect(MockMvcResultMatchers.model().attribute("notes", remainingNotes))

        verify { noteService.archiveNote(noteId) }
        verify { noteService.findAllNotes() }
    }
}
