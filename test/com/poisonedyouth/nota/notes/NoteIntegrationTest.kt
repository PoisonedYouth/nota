package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteIntegrationTest
    @Autowired
    constructor(
        private val noteRepository: NoteRepository,
        private val noteService: NoteService,
        private val userRepository: UserRepository,
    ) {

        private lateinit var testUser: User

        @BeforeEach
        fun setup() {
            noteRepository.deleteAll()
            userRepository.deleteAll()

            // Use a unique username for each test run to avoid conflicts
            val uniqueUsername = "testuser_integration_${System.currentTimeMillis()}"
            testUser = userRepository.save(
                User(
                    username = uniqueUsername,
                    password = "password",
                ),
            )
        }

        @Test
        fun `should return empty list when no notes exist`() {
            // When
            val notes = noteService.findAllNotes(testUser.id!!)

            // Then
            notes.size shouldBe 0
        }

        @Test
        fun `should return notes ordered by updatedAt desc`() {
            // Given
            val now = LocalDateTime.now()
            val note1 = Note(title = "Test Note 1", content = "Test Content 1", createdAt = now, updatedAt = now, user = testUser)
            val note2 =
                Note(title = "Test Note 2", content = "Test Content 2", createdAt = now, updatedAt = now.plusHours(1), user = testUser)
            noteRepository.saveAll(listOf(note1, note2))

            // When
            val notes = noteService.findAllNotes(testUser.id!!)

            // Then
            notes.size shouldBe 2
            notes[0].title shouldBe "Test Note 2"
            notes[1].title shouldBe "Test Note 1"
        }

        @Test
        fun `should convert Note entities to NoteDtos with correct methods`() {
            // Given
            val now = LocalDateTime.now()
            val note = Note(title = "Test Note", content = "Test Content", createdAt = now, updatedAt = now, user = testUser)
            val savedNote = noteRepository.save(note)

            // When
            val notes = noteService.findAllNotes(testUser.id!!)

            // Then
            notes.size shouldBe 1
            val noteDto = notes[0]
            noteDto.id shouldBe savedNote.id
            noteDto.title shouldBe savedNote.title
            noteDto.content shouldBe savedNote.content

            // Verify that the DTO methods work correctly
            val contentPreview = noteDto.getContentPreview()
            contentPreview shouldContain "Test Content"

            val formattedDate = noteDto.getFormattedDate()
            formattedDate shouldContain now.year.toString()
        }

        @Test
        fun `should create a new note`() {
            // Given
            val createNoteDto = CreateNoteDto(
                title = "New Test Note",
                content = "New Test Content",
            )

            // When
            val createdNote = noteService.createNote(createNoteDto, testUser.id!!)

            // Then
            createdNote.title shouldBe "New Test Note"
            createdNote.content shouldBe "New Test Content"

            // Verify the note was saved to the repository
            val allNotes = noteRepository.findAll()
            allNotes.size shouldBe 1
            allNotes[0].title shouldBe "New Test Note"
            allNotes[0].content shouldBe "New Test Content"
        }

        @Test
        fun `should find note by id`() {
            // Given
            val note = Note(title = "Test Note", content = "Test Content", user = testUser)
            val savedNote = noteRepository.save(note)

            // When
            val foundNote = noteService.findNoteById(savedNote.id!!, testUser.id!!)

            // Then
            foundNote?.id shouldBe savedNote.id
            foundNote?.title shouldBe savedNote.title
            foundNote?.content shouldBe savedNote.content
        }

        @Test
        fun `should return null when note with id does not exist`() {
            // When
            val foundNote = noteService.findNoteById(999L, testUser.id!!)

            // Then
            foundNote shouldBe null
        }
    }
