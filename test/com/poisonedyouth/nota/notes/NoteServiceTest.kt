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
        val note1 = Note(id = 1L, title = "Note 1", content = "Content 1", createdAt = now, updatedAt = now.plusHours(2), archived = false)
        val note2 = Note(id = 2L, title = "Note 2", content = "Content 2", createdAt = now, updatedAt = now.plusHours(1), archived = false)
        val note3 = Note(id = 3L, title = "Note 3", content = "Content 3", createdAt = now, updatedAt = now.plusHours(3), archived = false)

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
        val note = Note(id = noteId, title = "Test Note", content = "Content", createdAt = now, updatedAt = now, archived = false)

        every { noteRepository.findById(noteId) } returns Optional.of(note)
        every { noteRepository.save(any()) } returns note

        // When
        val result = noteService.archiveNote(noteId)

        // Then
        result shouldBe true
        note.archived shouldBe true
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
            Note(id = 1L, title = "Archived Note 1", content = "Content 1", createdAt = now, updatedAt = now.plusHours(2), archived = true)
        val archivedNote2 =
            Note(id = 2L, title = "Archived Note 2", content = "Content 2", createdAt = now, updatedAt = now.plusHours(1), archived = true)

        every { noteRepository.findAllByArchivedTrueOrderByUpdatedAtDesc() } returns listOf(archivedNote1, archivedNote2)

        // When
        val result = noteService.findAllArchivedNotes()

        // Then
        result.size shouldBe 2
        result[0].id shouldBe 1L
        result[1].id shouldBe 2L
    }
}
