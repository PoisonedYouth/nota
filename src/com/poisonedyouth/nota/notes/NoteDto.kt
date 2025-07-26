package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class NoteDto(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val archived: Boolean,
    val archivedAt: LocalDateTime?,
    val dueDate: LocalDateTime?,
    val userId: Long,
    val user: UserDto,
) {
    companion object {
        fun fromEntity(note: Note): NoteDto {
            return NoteDto(
                id = note.id ?: -1,
                title = note.title,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                archived = note.archived,
                archivedAt = note.archivedAt,
                dueDate = note.dueDate,
                userId = note.user.id ?: -1,
                user = UserDto.fromEntity(note.user),
            )
        }
    }

    fun getFormattedDate(): String {
        return updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    fun getFormattedDueDate(): String? {
        return dueDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    fun getContentPreview(maxLength: Int = 100): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength) + "..."
        } else {
            content
        }
    }

    // No-parameter version for Java interoperability (used by Thymeleaf)
    fun getContentPreview(): String {
        return getContentPreview(100)
    }
}
