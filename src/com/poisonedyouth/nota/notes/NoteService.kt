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
        )

        val savedNote = noteRepository.save(note)
        return NoteDto.fromEntity(savedNote)
    }

    fun findAllNotes(): List<NoteDto> {
        return noteRepository.findAllByOrderByUpdatedAtDesc()
            .map { NoteDto.fromEntity(it) }
    }

    fun findNoteById(id: Long): NoteDto? {
        return noteRepository.findById(id)
            .map { NoteDto.fromEntity(it) }
            .orElse(null)
    }
}
