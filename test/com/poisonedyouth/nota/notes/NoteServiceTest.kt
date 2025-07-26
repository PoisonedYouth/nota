package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.Optional

class NoteServiceTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var noteShareRepository: NoteShareRepository
    private lateinit var noteService: NoteService
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        noteRepository = mockk()
        userRepository = mockk()
        noteShareRepository = mockk()
        noteService = NoteService(noteRepository, userRepository, noteShareRepository)

        testUser = User(
            id = 1L,
            username = "testuser",
            password = "password",
        )
    }

    @Test
    fun `findAllNotes should return empty list when no notes exist`() {
        // Given
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalse(testUser, Sort.by(Sort.Direction.DESC, "updatedAt")) } returns emptyList()

        // When
        val result = noteService.findAllNotes(1L)

        // Then
        result.size shouldBe 0
    }

    @Test
    fun `findAllNotes should sort by title ascending when specified`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(
            id = 1L,
            title = "B Note",
            content = "Content 1",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )
        val note2 = Note(
            id = 2L,
            title = "A Note",
            content = "Content 2",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalse(testUser, Sort.by(Sort.Direction.ASC, "title")) } returns listOf(note2, note1)

        // When
        val result = noteService.findAllNotes(1L, "title", "asc")

        // Then
        result.size shouldBe 2
        result[0].title shouldBe "A Note"
        result[1].title shouldBe "B Note"
    }

    @Test
    fun `findAllNotes should sort by title descending when specified`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(
            id = 1L,
            title = "A Note",
            content = "Content 1",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )
        val note2 = Note(
            id = 2L,
            title = "B Note",
            content = "Content 2",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalse(testUser, Sort.by(Sort.Direction.DESC, "title")) } returns listOf(note2, note1)

        // When
        val result = noteService.findAllNotes(1L, "title", "desc")

        // Then
        result.size shouldBe 2
        result[0].title shouldBe "B Note"
        result[1].title shouldBe "A Note"
    }

    @Test
    fun `searchNotes should support sorting parameters`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(
            id = 1L,
            title = "A Important Note",
            content = "Content 1",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )
        val note2 = Note(
            id = 2L,
            title = "B Important Note",
            content = "Content 2",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every {
            noteRepository.findAllByUserAndArchivedFalseAndQuery(
                testUser,
                "important",
                Sort.by(Sort.Direction.ASC, "title"),
            )
        } returns listOf(note1, note2)

        // When
        val result = noteService.searchNotes("important", 1L, "title", "asc")

        // Then
        result.size shouldBe 2
        result[0].id shouldBe 1L
        result[1].id shouldBe 2L
    }

    @Test
    fun `searchNotes should return empty list when query is blank`() {
        // Given
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalse(testUser, Sort.by(Sort.Direction.DESC, "updatedAt")) } returns emptyList()

        // When
        val result = noteService.searchNotes("  ", 1L)

        // Then
        result.size shouldBe 0
        verify { noteRepository.findAllByUserAndArchivedFalse(testUser, Sort.by(Sort.Direction.DESC, "updatedAt")) }
    }

    @Test
    fun `createNote should throw exception when content is empty`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Test Title",
            content = "",
            dueDate = null,
        )

        // When & Then
        shouldThrow<IllegalArgumentException> {
            noteService.createNote(createNoteDto, 1L)
        }.message shouldBe "Note content cannot be empty"
    }

    @Test
    fun `createNote should throw exception when content is blank`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Test Title",
            content = "   ",
            dueDate = null,
        )

        // When & Then
        shouldThrow<IllegalArgumentException> {
            noteService.createNote(createNoteDto, 1L)
        }.message shouldBe "Note content cannot be empty"
    }

    @Test
    fun `createNote should create note when content is valid`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Test Title",
            content = "Valid content",
            dueDate = null,
        )
        val savedNote = Note(
            id = 1L,
            title = "Test Title",
            content = "Valid content",
            dueDate = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.title shouldBe "Test Title"
        result.content shouldBe "Valid content"
        verify { noteRepository.save(any()) }
    }

    @Test
    fun `updateNote should throw exception when content is empty`() {
        // Given
        val updateNoteDto = UpdateNoteDto(
            id = 1L,
            title = "Updated Title",
            content = "",
            dueDate = null,
        )

        // When & Then
        shouldThrow<IllegalArgumentException> {
            noteService.updateNote(updateNoteDto, 1L)
        }.message shouldBe "Note content cannot be empty"
    }

    @Test
    fun `updateNote should throw exception when content is blank`() {
        // Given
        val updateNoteDto = UpdateNoteDto(
            id = 1L,
            title = "Updated Title",
            content = "   ",
            dueDate = null,
        )

        // When & Then
        shouldThrow<IllegalArgumentException> {
            noteService.updateNote(updateNoteDto, 1L)
        }.message shouldBe "Note content cannot be empty"
    }

    @Test
    fun `updateNote should update note when content is valid`() {
        // Given
        val now = LocalDateTime.now()
        val existingNote = Note(
            id = 1L,
            title = "Original Title",
            content = "Original content",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )
        val updateNoteDto = UpdateNoteDto(
            id = 1L,
            title = "Updated Title",
            content = "Updated content",
            dueDate = null,
        )
        val updatedNote = Note(
            id = 1L,
            title = "Updated Title",
            content = "Updated content",
            dueDate = null,
            createdAt = now,
            updatedAt = LocalDateTime.now(),
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findByIdAndUser(1L, testUser) } returns existingNote
        every { noteRepository.save(any()) } returns updatedNote

        // When
        val result = noteService.updateNote(updateNoteDto, 1L)

        // Then
        result?.title shouldBe "Updated Title"
        result?.content shouldBe "Updated content"
        verify { noteRepository.save(any()) }
    }
}
