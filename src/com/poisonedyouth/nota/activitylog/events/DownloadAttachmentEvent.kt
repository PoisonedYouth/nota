package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a file attachment is downloaded from a note
 */
class DownloadAttachmentEvent(
    userId: Long,
    noteId: Long,
    attachmentId: Long,
    filename: String,
) : ActivityEvent(
    userId = userId,
    action = "DOWNLOAD",
    entityType = "ATTACHMENT",
    entityId = attachmentId,
    description = "Attachment downloaded: '$filename' from note $noteId",
)
