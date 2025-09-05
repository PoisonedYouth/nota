package com.poisonedyouth.nota.notes

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ShareNoteDto(
    @field:NotBlank(message = "Username is required")
    val username: String,
    @field:Pattern(regexp = "read|edit", message = "Permission must be 'read' or 'edit'")
    val permission: String = "read",
)

data class NoteShareDto(
    val id: Long,
    val noteId: Long,
    val noteTitle: String,
    val sharedWithUserId: Long,
    val sharedWithUsername: String,
    val sharedByUsername: String,
    val permission: String,
    val createdAt: String,
) {
    companion object {
        fun fromEntity(noteShare: NoteShare): NoteShareDto =
            NoteShareDto(
                id = noteShare.id!!,
                noteId = noteShare.note.id!!,
                noteTitle = noteShare.note.title,
                sharedWithUserId = noteShare.sharedWithUser.id!!,
                sharedWithUsername = noteShare.sharedWithUser.username,
                sharedByUsername = noteShare.sharedByUser.username,
                permission = noteShare.permission,
                createdAt = noteShare.createdAt.toString(),
            )
    }
}
