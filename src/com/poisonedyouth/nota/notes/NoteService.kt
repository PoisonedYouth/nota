package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
@Suppress("TooManyFunctions")
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val noteShareRepository: NoteShareRepository,
) {

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    private fun sanitizeHtmlContent(content: String): String {
        // Allow safe HTML tags commonly used by Quill editor
        val allowedTags = setOf(
            "p", "br", "strong", "b", "em", "i", "u", "s", "strike",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li",
            "blockquote", "code", "pre",
            "a",
        )

        // Remove script tags and other dangerous elements
        var sanitized = content
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<object[^>]*>.*?</object>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<embed[^>]*>.*?</embed>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<form[^>]*>.*?</form>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
            .replace(Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), "")

        // Remove any tags not in the allowed list
        sanitized = sanitized.replace(Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*>")) { matchResult ->
            val isClosing = matchResult.groupValues[1].isNotEmpty()
            val tagName = matchResult.groupValues[2].lowercase()

            if (allowedTags.contains(tagName)) {
                if (tagName == "a" && !isClosing) {
                    // For anchor tags, only allow href attribute and sanitize it
                    val href = Regex("href\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
                        .find(matchResult.value)?.groupValues?.get(1) ?: ""
                    if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("mailto:")) {
                        "<a href=\"$href\">"
                    } else {
                        "<a>"
                    }
                } else {
                    "<${if (isClosing) "/" else ""}$tagName>"
                }
            } else {
                ""
            }
        }

        return sanitized.trim()
    }

    fun createNote(createNoteDto: CreateNoteDto, userId: Long): NoteDto {
        if (createNoteDto.content.isBlank()) {
            throw IllegalArgumentException("Note content cannot be empty")
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val note = Note(
            title = createNoteDto.title,
            content = sanitizeHtmlContent(createNoteDto.content),
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
        if (updateNoteDto.content.isBlank()) {
            throw IllegalArgumentException("Note content cannot be empty")
        }

        val user = userRepository.findById(userId).orElse(null) ?: return null
        val note = noteRepository.findByIdAndUser(updateNoteDto.id, user) ?: return null

        val updatedNote = Note(
            id = note.id,
            title = updateNoteDto.title,
            content = sanitizeHtmlContent(updateNoteDto.content),
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

    // Sharing functionality
    fun shareNote(noteId: Long, shareNoteDto: ShareNoteDto, userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val note = noteRepository.findByIdAndUser(noteId, user) ?: return false

        val targetUser = userRepository.findByUsername(shareNoteDto.username) ?: return false

        // Don't allow sharing with self
        if (targetUser.id == userId) return false

        // Check if already shared
        if (noteShareRepository.existsByNoteAndSharedWithUser(note, targetUser)) {
            return false
        }

        val noteShare = NoteShare(
            note = note,
            sharedWithUser = targetUser,
            sharedByUser = user,
            permission = shareNoteDto.permission,
        )

        noteShareRepository.save(noteShare)
        return true
    }

    fun revokeNoteShare(noteId: Long, targetUserId: Long, userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val note = noteRepository.findByIdAndUser(noteId, user) ?: return false
        val targetUser = userRepository.findById(targetUserId).orElse(null) ?: return false

        noteShareRepository.deleteByNoteAndSharedWithUser(note, targetUser)
        return true
    }

    fun getNoteShares(noteId: Long, userId: Long): List<NoteShareDto> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptyList()
        val note = noteRepository.findByIdAndUser(noteId, user) ?: return emptyList()

        return noteShareRepository.findAllByNote(note)
            .map { NoteShareDto.fromEntity(it) }
    }

    fun findAllSharedNotes(userId: Long, sortBy: String = "updatedAt", sortOrder: String = "desc"): List<NoteDto> {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val sort = createSort(sortBy, sortOrder)
        return noteShareRepository.findAllSharedWithUser(user)
            .map { NoteDto.fromEntity(it) }
    }

    fun findAllAccessibleNotes(userId: Long, sortBy: String = "updatedAt", sortOrder: String = "desc"): List<NoteDto> {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val sort = createSort(sortBy, sortOrder)
        return noteRepository.findAllAccessibleByUserAndArchivedFalse(user, sort)
            .map { NoteDto.fromEntity(it) }
    }

    fun findAccessibleNoteById(id: Long, userId: Long): NoteDto? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        return noteRepository.findByIdAndAccessibleByUser(id, user)
            ?.let { NoteDto.fromEntity(it) }
    }

    fun searchAccessibleNotes(query: String, userId: Long, sortBy: String = "updatedAt", sortOrder: String = "desc"): List<NoteDto> {
        if (query.isBlank()) {
            return findAllAccessibleNotes(userId, sortBy, sortOrder)
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val sort = createSort(sortBy, sortOrder)
        return noteRepository.findAllAccessibleByUserAndArchivedFalseAndQuery(
            user,
            query.trim(),
            sort,
        ).map { NoteDto.fromEntity(it) }
    }

    fun canUserAccessNote(noteId: Long, userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        return noteRepository.findByIdAndAccessibleByUser(noteId, user) != null
    }
}
