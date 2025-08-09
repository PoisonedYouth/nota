package com.poisonedyouth.nota.activitylog.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Service for publishing activity events
 */
@Service
class ActivityEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun publishLoginEvent(userId: Long) {
        eventPublisher.publishEvent(LoginEvent(userId))
    }

    fun publishCreateNoteEvent(
        userId: Long,
        noteId: Long,
        noteTitle: String,
    ) {
        eventPublisher.publishEvent(CreateNoteEvent(userId, noteId, noteTitle))
    }

    fun publishUpdateNoteEvent(
        userId: Long,
        noteId: Long,
        noteTitle: String,
    ) {
        eventPublisher.publishEvent(UpdateNoteEvent(userId, noteId, noteTitle))
    }

    fun publishArchiveNoteEvent(
        userId: Long,
        noteId: Long,
        noteTitle: String,
    ) {
        eventPublisher.publishEvent(ArchiveNoteEvent(userId, noteId, noteTitle))
    }

    fun publishShareNoteEvent(
        userId: Long,
        noteId: Long,
        noteTitle: String,
        sharedWithUsername: String,
    ) {
        eventPublisher.publishEvent(ShareNoteEvent(userId, noteId, noteTitle, sharedWithUsername))
    }

    fun publishUploadAttachmentEvent(
        userId: Long,
        noteId: Long,
        attachmentId: Long,
        filename: String,
    ) {
        eventPublisher.publishEvent(UploadAttachmentEvent(userId, noteId, attachmentId, filename))
    }

    fun publishDownloadAttachmentEvent(
        userId: Long,
        noteId: Long,
        attachmentId: Long,
        filename: String,
    ) {
        eventPublisher.publishEvent(DownloadAttachmentEvent(userId, noteId, attachmentId, filename))
    }

    fun publishDeleteAttachmentEvent(
        userId: Long,
        noteId: Long,
        attachmentId: Long,
        filename: String,
    ) {
        eventPublisher.publishEvent(DeleteAttachmentEvent(userId, noteId, attachmentId, filename))
    }
}
