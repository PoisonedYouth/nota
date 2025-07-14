package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class NoteServiceTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var noteService: NoteService
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        noteRepository = mockk()
        userRepository = mockk()
        noteService = NoteService(noteRepository, userRepository)

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
        every { noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser) } returns emptyList()

        // When
        val result = noteService.findAllNotes(1L)

        // Then
        result.size shouldBe 0
    }

    @Test
    fun `findAllNotes should return list of notes ordered by updatedAt desc`() {
        // Given
        val now = LocalDateTime.now()
        val note1 =
            Note(
                id = 1L,
                title = "Note 1",
                content = "Content 1",
                dueDate = null,
                createdAt = now,
                updatedAt = now.plusHours(2),
                archived = false,
                archivedAt = null,
                user = testUser,
            )
        val note2 =
            Note(
                id = 2L,
                title = "Note 2",
                content = "Content 2",
                dueDate = null,
                createdAt = now,
                updatedAt = now.plusHours(1),
                archived = false,
                archivedAt = null,
                user = testUser,
            )
        val note3 =
            Note(
                id = 3L,
                title = "Note 3",
                content = "Content 3",
                dueDate = null,
                createdAt = now,
                updatedAt = now.plusHours(3),
                archived = false,
                archivedAt = null,
                user = testUser,
            )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser) } returns listOf(note3, note1, note2)

        // When
        val result = noteService.findAllNotes(1L)

        // Then
        result.size shouldBe 3
        result[0].id shouldBe 3L
        result[1].id shouldBe 1L
        result[2].id shouldBe 2L
    }

    @Test
    fun `archiveNote should set archived flag to true and return true when note exists`() {
        // Given
        val noteId = 1L
        val now = LocalDateTime.now()
        val note =
            Note(
                id = noteId,
                title = "Test Note",
                content = "Content",
                dueDate = null,
                createdAt = now,
                updatedAt = now,
                archived = false,
                archivedAt = null,
                user = testUser,
            )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findByIdAndUser(noteId, testUser) } returns note
        every { noteRepository.save(any()) } returns note

        // When
        val result = noteService.archiveNote(noteId, 1L)

        // Then
        result shouldBe true
        verify {
            noteRepository.save(
                match { savedNote ->
                    savedNote.archived == true && savedNote.archivedAt != null
                },
            )
        }
    }

    @Test
    fun `archiveNote should return false when note does not exist`() {
        // Given
        val noteId = 1L
        every { userRepository.findById(1L) } returns Optional.empty()

        // When
        val result = noteService.archiveNote(noteId, 1L)

        // Then
        result shouldBe false
        verify(exactly = 0) { noteRepository.save(any()) }
    }

    @Test
    fun `findAllArchivedNotes should return list of archived notes`() {
        // Given
        val now = LocalDateTime.now()
        val archivedNote1 =
            Note(
                id = 1L,
                title = "Archived Note 1",
                content = "Content 1",
                dueDate = null,
                createdAt = now,
                updatedAt = now.plusHours(2),
                archived = true,
                archivedAt = now.plusHours(2),
                user = testUser,
            )
        val archivedNote2 =
            Note(
                id = 2L,
                title = "Archived Note 2",
                content = "Content 2",
                dueDate = null,
                createdAt = now,
                updatedAt = now.plusHours(1),
                archived = true,
                archivedAt = now.plusHours(1),
                user = testUser,
            )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedTrueOrderByUpdatedAtDesc(testUser) } returns listOf(archivedNote1, archivedNote2)

        // When
        val result = noteService.findAllArchivedNotes(1L)

        // Then
        result.size shouldBe 2
        result[0].id shouldBe 1L
        result[1].id shouldBe 2L
    }

    @Test
    fun `createNote should create note with due date when provided`() {
        // Given
        val dueDate = LocalDateTime.now().plusDays(7)
        val createNoteDto = CreateNoteDto(title = "Test Note", content = "Test Content", dueDate = dueDate)
        val savedNote = Note(id = 1L, title = "Test Note", content = "Test Content", dueDate = dueDate, user = testUser)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.title shouldBe "Test Note"
        result.content shouldBe "Test Content"
        result.dueDate shouldBe dueDate
        verify { noteRepository.save(any()) }
    }

    @Test
    fun `createNote should create note without due date when not provided`() {
        // Given
        val createNoteDto = CreateNoteDto(title = "Test Note", content = "Test Content")
        val savedNote = Note(id = 1L, title = "Test Note", content = "Test Content", dueDate = null, user = testUser)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.title shouldBe "Test Note"
        result.content shouldBe "Test Content"
        result.dueDate shouldBe null
        verify { noteRepository.save(any()) }
    }

    @Test
    fun `searchNotes should return all notes when query is blank`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(
            id = 1L,
            title = "First Note",
            content = "Content 1",
            dueDate = null,
            createdAt = now,
            updatedAt = now.plusHours(2),
            archived = false,
            archivedAt = null,
            user = testUser,
        )
        val note2 = Note(
            id = 2L,
            title = "Second Note",
            content = "Content 2",
            dueDate = null,
            createdAt = now,
            updatedAt = now.plusHours(1),
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser) } returns listOf(note1, note2)

        // When
        val result = noteService.searchNotes("  ", 1L)

        // Then
        result.size shouldBe 2
        result[0].id shouldBe 1L
        result[1].id shouldBe 2L
        verify { noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser) }
    }

    @Test
    fun `searchNotes should return matching notes when query is provided`() {
        // Given
        val now = LocalDateTime.now()
        val matchingNote = Note(
            id = 1L,
            title = "Important Note",
            content = "This is important content",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "important",
                "important",
            )
        } returns listOf(matchingNote)

        // When
        val result = noteService.searchNotes("important", 1L)

        // Then
        result.size shouldBe 1
        result[0].id shouldBe 1L
        result[0].title shouldBe "Important Note"
        verify {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "important",
                "important",
            )
        }
    }

    @Test
    fun `searchNotes should return empty list when no matches found`() {
        // Given
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "nonexistent",
                "nonexistent",
            )
        } returns emptyList()

        // When
        val result = noteService.searchNotes("nonexistent", 1L)

        // Then
        result.size shouldBe 0
        verify {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "nonexistent",
                "nonexistent",
            )
        }
    }

    @Test
    fun `searchNotes should trim query before searching`() {
        // Given
        val now = LocalDateTime.now()
        val matchingNote = Note(
            id = 1L,
            title = "Test Note",
            content = "Test content",
            dueDate = null,
            createdAt = now,
            updatedAt = now,
            archived = false,
            archivedAt = null,
            user = testUser,
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "test",
                "test",
            )
        } returns listOf(matchingNote)

        // When
        val result = noteService.searchNotes("  test  ", 1L)

        // Then
        result.size shouldBe 1
        result[0].id shouldBe 1L
        verify {
            noteRepository.findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                testUser,
                "test",
                "test",
            )
        }
    }
}
