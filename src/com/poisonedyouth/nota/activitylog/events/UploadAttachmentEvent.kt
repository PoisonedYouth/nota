package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a file attachment is uploaded to a note
 */
class UploadAttachmentEvent(
    userId: Long,
    noteId: Long,
    attachmentId: Long,
    filename: String,
) : ActivityEvent(
    userId = userId,
    action = "UPLOAD",
    entityType = "ATTACHMENT",
    entityId = attachmentId,
    description = "Attachment uploaded: '$filename' to note $noteId",
)
