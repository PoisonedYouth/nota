package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class NoteAttachmentRepositoryTest
    @Autowired
    constructor(
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository,
        private val attachmentRepository: NoteAttachmentRepository,
    ) {
        private lateinit var user: User
        private lateinit var note: Note

        @BeforeEach
        fun setup() {
            attachmentRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()

            user = userRepository.save(User(username = "user_${System.currentTimeMillis()}", password = "pw"))
            note = noteRepository.save(Note(title = "Note", content = "content", user = user))
        }

        @Test
        fun `should save and load attachments for note`() {
            // Given
            val a1 = attachmentRepository.save(
                NoteAttachment(
                    note = note,
                    filename = "a.txt",
                    contentType = "text/plain",
                    fileSize = 3,
                    data = byteArrayOf(0x61, 0x62, 0x63),
                ),
            )
            val a2 = attachmentRepository.save(
                NoteAttachment(
                    note = note,
                    filename = "b.bin",
                    contentType = "application/octet-stream",
                    fileSize = 2,
                    data = byteArrayOf(0x01, 0x02),
                ),
            )

            // When
            val list = attachmentRepository.findAllByNoteIdOrderByCreatedAtDesc(note.id!!)

            // Then
            list shouldHaveSize 2
            list[0].id shouldBe a2.id
            list[1].id shouldBe a1.id

            val found = attachmentRepository.findByIdAndNoteId(a1.id!!, note.id!!)
            found?.filename shouldBe "a.txt"
        }
    }
