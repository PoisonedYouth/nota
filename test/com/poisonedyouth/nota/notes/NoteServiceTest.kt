package com.poisonedyouth.nota.notes

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
    private lateinit var noteService: NoteService

    @BeforeEach
    fun setup() {
        noteRepository = mockk()
        noteService = NoteService(noteRepository)
    }

    @Test
    fun `findAllNotes should return empty list when no notes exist`() {
        // Given
        every { noteRepository.findAllByArchivedFalseOrderByUpdatedAtDesc() } returns emptyList()

        // When
        val result = noteService.findAllNotes()

        // Then
        result.size shouldBe 0
    }

    @Test
    fun `findAllNotes should return list of notes ordered by updatedAt desc`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(id = 1L, title = "Note 1", content = "Content 1", dueDate = null, createdAt = now, updatedAt = now.plusHours(2), archived = false, archivedAt = null)
        val note2 = Note(id = 2L, title = "Note 2", content = "Content 2", dueDate = null, createdAt = now, updatedAt = now.plusHours(1), archived = false, archivedAt = null)
        val note3 = Note(id = 3L, title = "Note 3", content = "Content 3", dueDate = null, createdAt = now, updatedAt = now.plusHours(3), archived = false, archivedAt = null)

        every { noteRepository.findAllByArchivedFalseOrderByUpdatedAtDesc() } returns listOf(note3, note1, note2)

        // When
        val result = noteService.findAllNotes()

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
        val note = Note(id = noteId, title = "Test Note", content = "Content", dueDate = null, createdAt = now, updatedAt = now, archived = false, archivedAt = null)

        every { noteRepository.findById(noteId) } returns Optional.of(note)
        every { noteRepository.save(any()) } returns note

        // When
        val result = noteService.archiveNote(noteId)

        // Then
        result shouldBe true
        note.archived shouldBe true
        (note.archivedAt != null) shouldBe true
        verify { noteRepository.save(note) }
    }

    @Test
    fun `archiveNote should return false when note does not exist`() {
        // Given
        val noteId = 1L
        every { noteRepository.findById(noteId) } returns Optional.empty()

        // When
        val result = noteService.archiveNote(noteId)

        // Then
        result shouldBe false
        verify(exactly = 0) { noteRepository.save(any()) }
    }

    @Test
    fun `findAllArchivedNotes should return list of archived notes`() {
        // Given
        val now = LocalDateTime.now()
        val archivedNote1 =
            Note(id = 1L, title = "Archived Note 1", content = "Content 1", dueDate = null, createdAt = now, updatedAt = now.plusHours(2), archived = true, archivedAt = now.plusHours(2))
        val archivedNote2 =
            Note(id = 2L, title = "Archived Note 2", content = "Content 2", dueDate = null, createdAt = now, updatedAt = now.plusHours(1), archived = true, archivedAt = now.plusHours(1))

        every { noteRepository.findAllByArchivedTrueOrderByUpdatedAtDesc() } returns listOf(archivedNote1, archivedNote2)

        // When
        val result = noteService.findAllArchivedNotes()

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
        val savedNote = Note(id = 1L, title = "Test Note", content = "Test Content", dueDate = dueDate)

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto)

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
        val savedNote = Note(id = 1L, title = "Test Note", content = "Test Content", dueDate = null)

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto)

        // Then
        result.title shouldBe "Test Note"
        result.content shouldBe "Test Content"
        result.dueDate shouldBe null
        verify { noteRepository.save(any()) }
    }
}
