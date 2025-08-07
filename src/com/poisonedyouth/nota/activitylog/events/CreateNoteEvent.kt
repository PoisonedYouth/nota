package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a note is created
 */
class CreateNoteEvent(
    userId: Long,
    noteId: Long,
    noteTitle: String,
) : ActivityEvent(
        userId = userId,
        action = "CREATE",
        entityType = "NOTE",
        entityId = noteId,
        description = "Notiz erstellt: '$noteTitle'",
    )
