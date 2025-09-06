package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentDownloadHeadersIT
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val noteRepository: NoteRepository,
        private val attachmentRepository: NoteAttachmentRepository,
    ) {
        private lateinit var owner: User
        private lateinit var other: User

        @BeforeEach
        fun setup() {
            attachmentRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()
            owner = userRepository.save(User(username = "owner_${System.currentTimeMillis()}", password = "hash"))
            other = userRepository.save(User(username = "other_${System.currentTimeMillis()}", password = "hash"))
        }

        private fun sessionFor(user: User) = MockHttpSession().apply {
            setAttribute("currentUser", UserDto(id = user.id!!, username = user.username, mustChangePassword = false, role = UserRole.USER))
        }

        @Test
        fun `download includes safe headers and enforces access control`() {
            // Given
            val note = noteRepository.save(Note(title = "t", content = "c", user = owner))
            val att = attachmentRepository.save(
                NoteAttachment(
                    note = note,
                    filename = "weird name..txt",
                    contentType = MediaType.TEXT_PLAIN_VALUE,
                    fileSize = 5,
                    data = "hello".toByteArray(),
                ),
            )

            // When/Then: owner can download, headers are safe
            val res = mockMvc
                .perform(get("/notes/${note.id}/attachments/${att.id}/download").session(sessionFor(owner)))
                .andExpect(status().isOk)
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", org.hamcrest.Matchers.equalToIgnoringCase("nosniff")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn()

            // Spring may quote the filename; ensure it's present safely
            val cd = res.response.getHeader("Content-Disposition") ?: ""
            (cd.contains("filename=\"weird name..txt\"") || cd.contains("filename=weird name..txt")) shouldBe true

            // And: other user cannot download
            mockMvc
                .perform(get("/notes/${note.id}/attachments/${att.id}/download").session(sessionFor(other)))
                .andExpect(status().isNotFound)
        }
    }
