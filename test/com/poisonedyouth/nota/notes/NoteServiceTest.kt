package com.poisonedyouth.nota.notes

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
        every { noteRepository.findAllByOrderByUpdatedAtDesc() } returns emptyList()

        // When
        val result = noteService.findAllNotes()

        // Then
        result.size shouldBe 0
    }

    @Test
    fun `findAllNotes should return list of notes ordered by updatedAt desc`() {
        // Given
        val now = LocalDateTime.now()
        val note1 = Note(id = 1L, title = "Note 1", content = "Content 1", createdAt = now, updatedAt = now.plusHours(2))
        val note2 = Note(id = 2L, title = "Note 2", content = "Content 2", createdAt = now, updatedAt = now.plusHours(1))
        val note3 = Note(id = 3L, title = "Note 3", content = "Content 3", createdAt = now, updatedAt = now.plusHours(3))

        every { noteRepository.findAllByOrderByUpdatedAtDesc() } returns listOf(note3, note1, note2)

        // When
        val result = noteService.findAllNotes()

        // Then
        result.size shouldBe 3
        result[0].id shouldBe 3L
        result[1].id shouldBe 1L
        result[2].id shouldBe 2L
    }
}
