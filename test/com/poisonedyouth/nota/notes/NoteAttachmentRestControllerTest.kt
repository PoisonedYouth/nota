package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class NoteAttachmentRestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var attachmentService: NoteAttachmentService

    @BeforeEach
    fun setup() {
        attachmentService = mockk()
        val noteService: NoteService = mockk()
        val controller = NoteAttachmentRestController(attachmentService, noteService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun session(): MockHttpSession {
        val s = MockHttpSession()
        s.setAttribute("currentUser", UserDto(1L, "u", false, UserRole.USER))
        return s
    }

    @Test
    fun `unauthorized access is rejected`() {
        mockMvc.perform(get("/api/notes/1/attachments")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `upload, list, download and delete attachment`() {
        val file = MockMultipartFile("file", "readme.txt", "text/plain", "Hello".toByteArray())
        val dto = NoteAttachmentDto(1L, "readme.txt", "text/plain", 5, LocalDateTime.now(), 0L)

        every { attachmentService.addAttachment(1L, any(), 1L) } returns dto
        every { attachmentService.listAttachments(1L, 1L) } returns listOf(dto)
        every { attachmentService.getAttachment(1L, 1L, 1L) } returns NoteAttachment(
            id = 1L,
            note = mockk(relaxed = true),
            filename = "readme.txt",
            contentType = "text/plain",
            fileSize = 5,
            data = "Hello".toByteArray(),
        )
        every { attachmentService.deleteAttachment(1L, 1L, 1L) } returns true

        mockMvc
            .perform(multipart("/api/notes/1/attachments").file(file).session(session()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.filename").value("readme.txt"))

        mockMvc
            .perform(get("/api/notes/1/attachments").session(session()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))

        mockMvc
            .perform(get("/api/notes/1/attachments/1/download").session(session()))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("readme.txt")))

        mockMvc
            .perform(delete("/api/notes/1/attachments/1").session(session()))
            .andExpect(status().isOk)

        verify { attachmentService.addAttachment(1L, any(), 1L) }
        verify { attachmentService.listAttachments(1L, 1L) }
        verify { attachmentService.getAttachment(1L, 1L, 1L) }
        verify { attachmentService.deleteAttachment(1L, 1L, 1L) }
    }
}
