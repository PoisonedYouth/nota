package com.poisonedyouth.nota.notes

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NoteService(
    private val noteRepository: NoteRepository,
) {

    fun createNote(createNoteDto: CreateNoteDto): NoteDto {
        val note = Note(
            title = createNoteDto.title,
            content = createNoteDto.content,
            dueDate = createNoteDto.dueDate,
        )

        val savedNote = noteRepository.save(note)
        return NoteDto.fromEntity(savedNote)
    }

    fun findAllNotes(): List<NoteDto> {
        return noteRepository.findAllByArchivedFalseOrderByUpdatedAtDesc()
            .map { NoteDto.fromEntity(it) }
    }

    fun findNoteById(id: Long): NoteDto? {
        return noteRepository.findById(id)
            .map { NoteDto.fromEntity(it) }
            .orElse(null)
    }

    fun archiveNote(id: Long): Boolean {
        val note = noteRepository.findById(id).orElse(null) ?: return false
        note.archived = true
        note.archivedAt = java.time.LocalDateTime.now()
        noteRepository.save(note)
        return true
    }

    fun findAllArchivedNotes(): List<NoteDto> {
        return noteRepository.findAllByArchivedTrueOrderByUpdatedAtDesc()
            .map { NoteDto.fromEntity(it) }
    }
}
