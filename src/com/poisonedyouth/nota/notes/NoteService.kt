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
        val archivedNote = Note(
            id = note.id,
            title = note.title,
            content = note.content,
            dueDate = note.dueDate,
            createdAt = note.createdAt,
            updatedAt = java.time.LocalDateTime.now(),
            archived = true,
            archivedAt = java.time.LocalDateTime.now(),
        )
        noteRepository.save(archivedNote)
        return true
    }

    fun findAllArchivedNotes(): List<NoteDto> {
        return noteRepository.findAllByArchivedTrueOrderByUpdatedAtDesc()
            .map { NoteDto.fromEntity(it) }
    }

    fun updateNote(updateNoteDto: UpdateNoteDto): NoteDto? {
        val note = noteRepository.findById(updateNoteDto.id).orElse(null) ?: return null

        val updatedNote = Note(
            id = note.id,
            title = updateNoteDto.title,
            content = updateNoteDto.content,
            dueDate = updateNoteDto.dueDate,
            createdAt = note.createdAt,
            updatedAt = java.time.LocalDateTime.now(),
            archived = note.archived,
            archivedAt = note.archivedAt,
        )

        val savedNote = noteRepository.save(updatedNote)
        return NoteDto.fromEntity(savedNote)
    }

    fun searchNotes(query: String): List<NoteDto> {
        if (query.isBlank()) {
            return findAllNotes()
        }

        return noteRepository.findAllByArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
            query.trim(),
            query.trim(),
        ).map { NoteDto.fromEntity(it) }
    }
}
