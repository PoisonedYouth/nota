package com.poisonedyouth.nota.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class NoteSharingRestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var noteService: NoteService
    private lateinit var activityEventPublisher: ActivityEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        noteService = mockk()
        activityEventPublisher = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        val controller = NoteRestController(noteService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun createSession(): MockHttpSession {
        val session = MockHttpSession()
        val user = UserDto(1L, "owner", false, UserRole.USER)
        session.setAttribute("currentUser", user)
        return session
    }

    @Test
    fun `should share note with user`() {
        val session = createSession()
        val dto = ShareNoteDto("targetUser", "read")
        every { noteService.shareNote(10L, dto, 1L) } returns true

        mockMvc
            .perform(
                post("/api/notes/10/share").session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Note shared successfully"))
    }

    @Test
    fun `should list note shares`() {
        val session = createSession()
        every { noteService.getNoteShares(10L, 1L) } returns emptyList()

        mockMvc
            .perform(get("/api/notes/10/shares").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shares").isArray)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `should revoke note share`() {
        val session = createSession()
        every { noteService.revokeNoteShare(10L, 2L, 1L) } returns true

        mockMvc
            .perform(delete("/api/notes/10/shares/2").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Share revoked successfully"))
    }

    @Test
    fun `should list archived notes`() {
        val session = createSession()
        every { noteService.findAllArchivedNotes(1L) } returns emptyList()

        mockMvc
            .perform(get("/api/notes/archived").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").isArray)
            .andExpect(jsonPath("$.count").value(0))
    }
}
