package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a note is archived
 */
class ArchiveNoteEvent(
    userId: Long,
    noteId: Long,
    noteTitle: String,
) : ActivityEvent(
    userId = userId,
    action = "ARCHIVE",
    entityType = "NOTE",
    entityId = noteId,
    description = "Note archived: '$noteTitle'",
)
