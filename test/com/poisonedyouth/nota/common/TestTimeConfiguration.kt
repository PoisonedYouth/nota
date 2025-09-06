package com.poisonedyouth.nota.common

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Test configuration for providing deterministic time in tests
 */
@TestConfiguration
class TestTimeConfiguration {

    @Bean
    @Primary
    fun testClock(): Clock = Clock.fixed(
        Instant.parse("2024-01-15T10:30:00Z"),
        ZoneId.of("UTC"),
    )
}

/**
 * Utility class for managing deterministic time in tests
 */
object TestTimeUtils {

    /**
     * Fixed instant for deterministic testing: 2024-01-15T10:30:00Z
     */
    val FIXED_INSTANT: Instant = Instant.parse("2024-01-15T10:30:00Z")

    /**
     * Fixed clock for deterministic testing
     */
    val FIXED_CLOCK: Clock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"))

    /**
     * Fixed LocalDateTime for testing: 2024-01-15T10:30:00
     */
    val FIXED_TIME: LocalDateTime = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneId.of("UTC"))

    /**
     * Creates a clock that advances from the fixed time
     */
    fun tickingClock(startTime: Instant = FIXED_INSTANT): Clock =
        Clock.tick(Clock.system(ZoneId.of("UTC")), java.time.Duration.ofSeconds(1))

    /**
     * Creates a clock offset by the specified duration from the fixed time
     */
    fun offsetClock(offset: java.time.Duration): Clock =
        Clock.fixed(FIXED_INSTANT.plus(offset), ZoneId.of("UTC"))

    /**
     * Creates a LocalDateTime offset by the specified duration from the fixed time
     */
    fun offsetTime(offset: java.time.Duration): LocalDateTime =
        LocalDateTime.ofInstant(FIXED_INSTANT.plus(offset), ZoneId.of("UTC"))

    /**
     * Helper for creating LocalDateTime instances that are deterministic
     */
    fun createTestTime(
        year: Int = 2024,
        month: Int = 1,
        day: Int = 15,
        hour: Int = 10,
        minute: Int = 30,
        second: Int = 0,
    ): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute, second)
}

/**
 * Builder for creating test entities with deterministic timestamps
 */
object TestEntityBuilders {

    fun testUser(
        id: Long = 1L,
        username: String = "testuser",
        password: String = "password",
        clock: Clock = TestTimeUtils.FIXED_CLOCK,
    ) = com.poisonedyouth.nota.user.User(
        id = id,
        username = username,
        password = password,
        createdAt = LocalDateTime.now(clock),
        updatedAt = LocalDateTime.now(clock),
    )

    fun testNote(
        id: Long = 1L,
        title: String = "Test Note",
        content: String = "Test content",
        user: com.poisonedyouth.nota.user.User,
        clock: Clock = TestTimeUtils.FIXED_CLOCK,
    ) = com.poisonedyouth.nota.notes.Note(
        id = id,
        title = title,
        content = content,
        user = user,
        createdAt = LocalDateTime.now(clock),
        updatedAt = LocalDateTime.now(clock),
    )

    fun testNoteAttachment(
        id: Long = 1L,
        note: com.poisonedyouth.nota.notes.Note,
        filename: String = "test.txt",
        contentType: String = "text/plain",
        fileSize: Long = 100L,
        data: ByteArray = "test content".toByteArray(),
        clock: Clock = TestTimeUtils.FIXED_CLOCK,
    ) = com.poisonedyouth.nota.notes.NoteAttachment(
        id = id,
        note = note,
        filename = filename,
        contentType = contentType,
        fileSize = fileSize,
        data = data,
        createdAt = LocalDateTime.now(clock),
    )
}
