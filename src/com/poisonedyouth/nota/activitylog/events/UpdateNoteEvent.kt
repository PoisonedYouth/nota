package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a note is updated
 */
class UpdateNoteEvent(
    userId: Long,
    noteId: Long,
    noteTitle: String,
) : ActivityEvent(
        userId = userId,
        action = "UPDATE",
        entityType = "NOTE",
        entityId = noteId,
        description = "Notiz bearbeitet: '$noteTitle'",
    )
