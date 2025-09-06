package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NoteDtoTest {
    private val testUser =
        User(
            id = 1L,
            username = "testuser",
            password = "password",
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private val testUserDto = UserDto.fromEntity(testUser)

    @Test
    fun `isOverdue should return true when due date is in the past`() {
        // Given
        val pastDueDate = LocalDateTime.now().minusHours(1)
        val noteDto = createNoteDto(dueDate = pastDueDate)

        // When
        val result = noteDto.isOverdue()

        // Then
        result shouldBe true
    }

    @Test
    fun `isOverdue should return false when due date is in the future`() {
        // Given
        val futureDueDate = LocalDateTime.now().plusHours(1)
        val noteDto = createNoteDto(dueDate = futureDueDate)

        // When
        val result = noteDto.isOverdue()

        // Then
        result shouldBe false
    }

    @Test
    fun `isOverdue should return false when due date is null`() {
        // Given
        val noteDto = createNoteDto(dueDate = null)

        // When
        val result = noteDto.isOverdue()

        // Then
        result shouldBe false
    }

    @Test
    fun `isDueSoon should return true when due date is within 24 hours`() {
        // Given
        val dueDateIn12Hours = LocalDateTime.now().plusHours(12)
        val noteDto = createNoteDto(dueDate = dueDateIn12Hours)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe true
    }

    @Test
    fun `isDueSoon should return false when due date is more than 24 hours away`() {
        // Given
        val dueDateIn48Hours = LocalDateTime.now().plusHours(48)
        val noteDto = createNoteDto(dueDate = dueDateIn48Hours)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe false
    }

    @Test
    fun `isDueSoon should return false when due date is in the past`() {
        // Given
        val pastDueDate = LocalDateTime.now().minusHours(1)
        val noteDto = createNoteDto(dueDate = pastDueDate)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe false
    }

    @Test
    fun `isDueSoon should return false when due date is null`() {
        // Given
        val noteDto = createNoteDto(dueDate = null)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe false
    }

    @Test
    fun `isDueSoon should return true when due date is exactly 23 hours away`() {
        // Given
        val dueDateIn23Hours = LocalDateTime.now().plusHours(23)
        val noteDto = createNoteDto(dueDate = dueDateIn23Hours)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe true
    }

    @Test
    fun `isDueSoon should return false when due date is exactly 25 hours away`() {
        // Given
        val dueDateIn25Hours = LocalDateTime.now().plusHours(25)
        val noteDto = createNoteDto(dueDate = dueDateIn25Hours)

        // When
        val result = noteDto.isDueSoon()

        // Then
        result shouldBe false
    }

    private fun createNoteDto(dueDate: LocalDateTime?): NoteDto =
        NoteDto(
            id = 1L,
            title = "Test Note",
            content = "Test content",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now(),
            archived = false,
            archivedAt = null,
            dueDate = dueDate,
            userId = 1L,
            user = testUserDto,
            version = 0L,
        )
}
