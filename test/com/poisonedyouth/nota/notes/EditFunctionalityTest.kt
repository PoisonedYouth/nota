package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
class EditFunctionalityTest {

    @Autowired
    private lateinit var noteService: NoteService

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()

        testUser = userRepository.save(
            User(
                username = "testuser",
                password = "password",
            ),
        )
    }

    @Test
    fun `should update note successfully`() {
        // Create a note first
        val createDto = CreateNoteDto(
            title = "Original Title",
            content = "Original Content",
            dueDate = LocalDateTime.of(2024, 12, 31, 23, 59),
        )
        val createdNote = noteService.createNote(createDto, testUser.id!!)

        // Update the note
        val updateDto = UpdateNoteDto(
            id = createdNote.id,
            title = "Updated Title",
            content = "Updated Content",
            dueDate = LocalDateTime.of(2025, 1, 15, 12, 0),
        )
        val updatedNote = noteService.updateNote(updateDto, testUser.id!!)

        // Verify the update
        updatedNote shouldNotBe null
        updatedNote!!.title shouldBe "Updated Title"
        updatedNote.content shouldBe "Updated Content"
        updatedNote.dueDate shouldBe LocalDateTime.of(2025, 1, 15, 12, 0)
        updatedNote.id shouldBe createdNote.id
        // Check that updatedAt is either after or equal (in case of very fast execution)
        (updatedNote.updatedAt.isAfter(createdNote.updatedAt) || updatedNote.updatedAt.isEqual(createdNote.updatedAt)) shouldBe true
    }

    @Test
    fun `should return null when updating non-existent note`() {
        val updateDto = UpdateNoteDto(
            id = 999L,
            title = "Non-existent",
            content = "Content",
            dueDate = null,
        )
        val result = noteService.updateNote(updateDto, testUser.id!!)
        result shouldBe null
    }

    @Test
    fun `should update note with null due date`() {
        // Create a note with due date
        val createDto = CreateNoteDto(
            title = "Test Title",
            content = "Test Content",
            dueDate = LocalDateTime.now(),
        )
        val createdNote = noteService.createNote(createDto, testUser.id!!)

        // Update to remove due date
        val updateDto = UpdateNoteDto(
            id = createdNote.id,
            title = "Updated Title",
            content = "Updated Content",
            dueDate = null,
        )
        val updatedNote = noteService.updateNote(updateDto, testUser.id!!)

        // Verify the update
        updatedNote shouldNotBe null
        updatedNote!!.title shouldBe "Updated Title"
        updatedNote.content shouldBe "Updated Content"
        updatedNote.dueDate shouldBe null
    }
}
