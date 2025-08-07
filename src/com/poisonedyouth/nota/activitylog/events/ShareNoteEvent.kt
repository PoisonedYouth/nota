package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a note is shared with another user
 */
class ShareNoteEvent(
    userId: Long,
    noteId: Long,
    noteTitle: String,
    sharedWithUsername: String,
) : ActivityEvent(
    userId = userId,
    action = "SHARE",
    entityType = "NOTE",
    entityId = noteId,
    description = "Notiz geteilt: '$noteTitle' mit Benutzer '$sharedWithUsername'",
)
