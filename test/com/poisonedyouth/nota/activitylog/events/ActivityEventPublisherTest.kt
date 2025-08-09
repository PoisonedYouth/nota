package com.poisonedyouth.nota.activitylog.events

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class ActivityEventPublisherTest {
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    private lateinit var activityEventPublisher: ActivityEventPublisher

    @BeforeEach
    fun setup() {
        applicationEventPublisher = mockk(relaxed = true)
        activityEventPublisher = ActivityEventPublisher(applicationEventPublisher)
    }

    @Test
    fun `publishLoginEvent should publish LoginEvent`() {
        // Given
        val userId = 1L
        val eventSlot = slot<LoginEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishLoginEvent(userId)

        // Then
        verify { applicationEventPublisher.publishEvent(any<LoginEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "LOGIN"
        capturedEvent.entityType shouldBe "USER"
        capturedEvent.entityId shouldBe userId
    }

    @Test
    fun `publishCreateNoteEvent should publish CreateNoteEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val noteTitle = "Test Note"
        val eventSlot = slot<CreateNoteEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishCreateNoteEvent(userId, noteId, noteTitle)

        // Then
        verify { applicationEventPublisher.publishEvent(any<CreateNoteEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "CREATE"
        capturedEvent.entityType shouldBe "NOTE"
        capturedEvent.entityId shouldBe noteId
    }

    @Test
    fun `publishUpdateNoteEvent should publish UpdateNoteEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val noteTitle = "Updated Note"
        val eventSlot = slot<UpdateNoteEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishUpdateNoteEvent(userId, noteId, noteTitle)

        // Then
        verify { applicationEventPublisher.publishEvent(any<UpdateNoteEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "UPDATE"
        capturedEvent.entityType shouldBe "NOTE"
        capturedEvent.entityId shouldBe noteId
    }

    @Test
    fun `publishArchiveNoteEvent should publish ArchiveNoteEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val noteTitle = "Archived Note"
        val eventSlot = slot<ArchiveNoteEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishArchiveNoteEvent(userId, noteId, noteTitle)

        // Then
        verify { applicationEventPublisher.publishEvent(any<ArchiveNoteEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "ARCHIVE"
        capturedEvent.entityType shouldBe "NOTE"
        capturedEvent.entityId shouldBe noteId
    }

    @Test
    fun `publishShareNoteEvent should publish ShareNoteEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val noteTitle = "Shared Note"
        val sharedWithUsername = "recipient"
        val eventSlot = slot<ShareNoteEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishShareNoteEvent(userId, noteId, noteTitle, sharedWithUsername)

        // Then
        verify { applicationEventPublisher.publishEvent(any<ShareNoteEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "SHARE"
        capturedEvent.entityType shouldBe "NOTE"
        capturedEvent.entityId shouldBe noteId
    }

    @Test
    fun `publishUploadAttachmentEvent should publish UploadAttachmentEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val attachmentId = 3L
        val filename = "document.pdf"
        val eventSlot = slot<UploadAttachmentEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishUploadAttachmentEvent(userId, noteId, attachmentId, filename)

        // Then
        verify { applicationEventPublisher.publishEvent(any<UploadAttachmentEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "UPLOAD"
        capturedEvent.entityType shouldBe "ATTACHMENT"
        capturedEvent.entityId shouldBe attachmentId
        capturedEvent.description shouldBe "Attachment uploaded: '$filename' to note $noteId"
    }

    @Test
    fun `publishDownloadAttachmentEvent should publish DownloadAttachmentEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val attachmentId = 3L
        val filename = "report.xlsx"
        val eventSlot = slot<DownloadAttachmentEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishDownloadAttachmentEvent(userId, noteId, attachmentId, filename)

        // Then
        verify { applicationEventPublisher.publishEvent(any<DownloadAttachmentEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "DOWNLOAD"
        capturedEvent.entityType shouldBe "ATTACHMENT"
        capturedEvent.entityId shouldBe attachmentId
        capturedEvent.description shouldBe "Attachment downloaded: '$filename' from note $noteId"
    }

    @Test
    fun `publishDeleteAttachmentEvent should publish DeleteAttachmentEvent`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val attachmentId = 3L
        val filename = "image.png"
        val eventSlot = slot<DeleteAttachmentEvent>()

        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } returns Unit

        // When
        activityEventPublisher.publishDeleteAttachmentEvent(userId, noteId, attachmentId, filename)

        // Then
        verify { applicationEventPublisher.publishEvent(any<DeleteAttachmentEvent>()) }
        val capturedEvent = eventSlot.captured
        capturedEvent.userId shouldBe userId
        capturedEvent.action shouldBe "DELETE"
        capturedEvent.entityType shouldBe "ATTACHMENT"
        capturedEvent.entityId shouldBe attachmentId
        capturedEvent.description shouldBe "Attachment deleted: '$filename' from note $noteId"
    }

    @Test
    fun `should handle multiple events published in sequence`() {
        // Given
        val userId = 1L
        val noteId = 2L
        val noteTitle = "Test Note"

        // When
        activityEventPublisher.publishCreateNoteEvent(userId, noteId, noteTitle)
        activityEventPublisher.publishUpdateNoteEvent(userId, noteId, "Updated Title")
        activityEventPublisher.publishArchiveNoteEvent(userId, noteId, "Final Title")

        // Then
        verify { applicationEventPublisher.publishEvent(any<CreateNoteEvent>()) }
        verify { applicationEventPublisher.publishEvent(any<UpdateNoteEvent>()) }
        verify { applicationEventPublisher.publishEvent(any<ArchiveNoteEvent>()) }
    }
}
