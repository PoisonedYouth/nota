package com.poisonedyouth.nota.notes

import io.mockk.every
import io.mockk.mockk
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
            NoteDto(id = 1L, title = "Note 1", content = "Content 1", createdAt = now, updatedAt = now),
            NoteDto(id = 2L, title = "Note 2", content = "Content 2", createdAt = now, updatedAt = now),
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
}
