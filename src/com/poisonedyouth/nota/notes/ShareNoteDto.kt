package com.poisonedyouth.nota.notes

data class ShareNoteDto(
    val username: String,
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
