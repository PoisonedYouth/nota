package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
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
}
