package com.poisonedyouth.nota.activitylog.events

import com.poisonedyouth.nota.activitylog.ActivityLogService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ActivityEventListenerTest {

    private lateinit var activityLogService: ActivityLogService
    private lateinit var activityEventListener: ActivityEventListener

    @BeforeEach
    fun setup() {
        activityLogService = mockk()
        activityEventListener = ActivityEventListener(activityLogService)
    }

    @Test
    fun `handleActivityEvent should log LoginEvent`() {
        // Given
        val loginEvent = LoginEvent(userId = 1L)
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "LOGIN",
                entityType = "USER",
                entityId = 1L,
                description = "Benutzer angemeldet",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(loginEvent)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "LOGIN",
                entityType = "USER",
                entityId = 1L,
                description = "Benutzer angemeldet",
            )
        }
    }

    @Test
    fun `handleActivityEvent should log CreateNoteEvent`() {
        // Given
        val createEvent = CreateNoteEvent(
            userId = 1L,
            noteId = 2L,
            noteTitle = "Test Note",
        )
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz erstellt: 'Test Note'",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(createEvent)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz erstellt: 'Test Note'",
            )
        }
    }

    @Test
    fun `handleActivityEvent should log UpdateNoteEvent`() {
        // Given
        val updateEvent = UpdateNoteEvent(
            userId = 1L,
            noteId = 2L,
            noteTitle = "Updated Note",
        )
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "UPDATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz bearbeitet: 'Updated Note'",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(updateEvent)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "UPDATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz bearbeitet: 'Updated Note'",
            )
        }
    }

    @Test
    fun `handleActivityEvent should log ArchiveNoteEvent`() {
        // Given
        val archiveEvent = ArchiveNoteEvent(
            userId = 1L,
            noteId = 2L,
            noteTitle = "Archived Note",
        )
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "ARCHIVE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz archiviert: 'Archived Note'",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(archiveEvent)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "ARCHIVE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz archiviert: 'Archived Note'",
            )
        }
    }

    @Test
    fun `handleActivityEvent should log ShareNoteEvent`() {
        // Given
        val shareEvent = ShareNoteEvent(
            userId = 1L,
            noteId = 2L,
            noteTitle = "Shared Note",
            sharedWithUsername = "recipient",
        )
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "SHARE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz geteilt: 'Shared Note' mit Benutzer 'recipient'",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(shareEvent)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "SHARE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz geteilt: 'Shared Note' mit Benutzer 'recipient'",
            )
        }
    }

    @Test
    fun `should handle multiple events in sequence`() {
        // Given
        val event1 = LoginEvent(userId = 1L)
        val event2 = CreateNoteEvent(userId = 1L, noteId = 2L, noteTitle = "Note 1")
        val event3 = UpdateNoteEvent(userId = 1L, noteId = 2L, noteTitle = "Updated Note 1")

        every { activityLogService.logActivity(any(), any(), any(), any(), any()) } just Runs

        // When
        activityEventListener.handleActivityEvent(event1)
        activityEventListener.handleActivityEvent(event2)
        activityEventListener.handleActivityEvent(event3)

        // Then
        verify(exactly = 3) { activityLogService.logActivity(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should handle events with empty or null titles gracefully`() {
        // Given
        val eventWithEmptyTitle = CreateNoteEvent(
            userId = 1L,
            noteId = 2L,
            noteTitle = "",
        )
        every {
            activityLogService.logActivity(
                userId = 1L,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz erstellt: ''",
            )
        } just Runs

        // When
        activityEventListener.handleActivityEvent(eventWithEmptyTitle)

        // Then
        verify {
            activityLogService.logActivity(
                userId = 1L,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 2L,
                description = "Notiz erstellt: ''",
            )
        }
    }
}
