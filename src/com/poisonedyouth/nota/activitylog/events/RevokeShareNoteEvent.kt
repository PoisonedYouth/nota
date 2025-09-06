package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a note share is revoked from a user
 */
class RevokeShareNoteEvent(
    userId: Long,
    noteId: Long,
    noteTitle: String,
    sharedWithUserId: Long,
) : ActivityEvent(
    userId = userId,
    action = "REVOKE_SHARE",
    entityType = "NOTE",
    entityId = noteId,
    description = "Note share revoked: '$noteTitle' for user id $sharedWithUserId",
)
