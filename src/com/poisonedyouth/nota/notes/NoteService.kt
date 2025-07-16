package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
) {

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    fun createNote(createNoteDto: CreateNoteDto, userId: Long): NoteDto {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val note = Note(
            title = createNoteDto.title,
            content = createNoteDto.content,
            dueDate = createNoteDto.dueDate,
            user = user,
        )

        val savedNote = noteRepository.save(note)
        return NoteDto.fromEntity(savedNote)
    }

    fun findAllNotes(userId: Long, sortBy: String = "updatedAt", sortOrder: String = "desc"): List<NoteDto> {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val sort = createSort(sortBy, sortOrder)
        return noteRepository.findAllByUserAndArchivedFalse(user, sort)
            .map { NoteDto.fromEntity(it) }
    }

    fun findNoteById(id: Long, userId: Long): NoteDto? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        return noteRepository.findByIdAndUser(id, user)
            ?.let { NoteDto.fromEntity(it) }
    }

    fun archiveNote(id: Long, userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val note = noteRepository.findByIdAndUser(id, user) ?: return false
        val archivedNote = Note(
            id = note.id,
            title = note.title,
            content = note.content,
            dueDate = note.dueDate,
            createdAt = note.createdAt,
            updatedAt = java.time.LocalDateTime.now(),
            archived = true,
            archivedAt = java.time.LocalDateTime.now(),
            user = note.user,
        )
        noteRepository.save(archivedNote)
        return true
    }

    fun findAllArchivedNotes(userId: Long): List<NoteDto> {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        return noteRepository.findAllByUserAndArchivedTrueOrderByUpdatedAtDesc(user)
            .map { NoteDto.fromEntity(it) }
    }

    fun updateNote(updateNoteDto: UpdateNoteDto, userId: Long): NoteDto? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val note = noteRepository.findByIdAndUser(updateNoteDto.id, user) ?: return null

        val updatedNote = Note(
            id = note.id,
            title = updateNoteDto.title,
            content = updateNoteDto.content,
            dueDate = updateNoteDto.dueDate,
            createdAt = note.createdAt,
            updatedAt = java.time.LocalDateTime.now(),
            archived = note.archived,
            archivedAt = note.archivedAt,
            user = note.user,
        )

        val savedNote = noteRepository.save(updatedNote)
        return NoteDto.fromEntity(savedNote)
    }

    fun searchNotes(query: String, userId: Long, sortBy: String = "updatedAt", sortOrder: String = "desc"): List<NoteDto> {
        if (query.isBlank()) {
            return findAllNotes(userId, sortBy, sortOrder)
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val sort = createSort(sortBy, sortOrder)
        return noteRepository.findAllByUserAndArchivedFalseAndQuery(
            user,
            query.trim(),
            sort,
        ).map { NoteDto.fromEntity(it) }
    }
}
