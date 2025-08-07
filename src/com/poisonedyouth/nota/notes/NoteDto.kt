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
        fun fromEntity(note: Note): NoteDto =
            NoteDto(
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

    fun getFormattedDate(): String = updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    fun getFormattedDueDate(): String? = dueDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    fun isOverdue(): Boolean = dueDate?.isBefore(LocalDateTime.now()) ?: false

    fun isDueSoon(): Boolean {
        if (dueDate == null) return false
        val now = LocalDateTime.now()
        val twentyFourHoursFromNow = now.plusHours(24)
        return dueDate.isAfter(now) && dueDate.isBefore(twentyFourHoursFromNow)
    }

    fun getContentPreview(maxLength: Int = 100): String {
        // Strip HTML tags for preview
        val plainText =
            content
                .replace(Regex("<[^>]*>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim()

        return if (plainText.length > maxLength) {
            plainText.substring(0, maxLength) + "..."
        } else {
            plainText
        }
    }

    // No-parameter version for Java interoperability (used by Thymeleaf)
    fun getContentPreview(): String = getContentPreview(100)
}
