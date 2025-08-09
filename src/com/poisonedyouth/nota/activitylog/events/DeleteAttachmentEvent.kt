package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a file attachment is deleted from a note
 */
class DeleteAttachmentEvent(
    userId: Long,
    noteId: Long,
    attachmentId: Long,
    filename: String,
) : ActivityEvent(
    userId = userId,
    action = "DELETE",
    entityType = "ATTACHMENT",
    entityId = attachmentId,
    description = "Attachment deleted: '$filename' from note $noteId",
)