package com.poisonedyouth.nota.activitylog.events

import com.poisonedyouth.nota.activitylog.ActivityLogRepository
import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityEventIntegrationTest
    @Autowired
    constructor(
        private val activityEventPublisher: ActivityEventPublisher,
        private val activityLogRepository: ActivityLogRepository,
        private val userRepository: UserRepository,
    ) {
        private lateinit var testUser: User

        @BeforeEach
        fun setup() {
            activityLogRepository.deleteAll()
            userRepository.deleteAll()

            testUser =
                userRepository.save(
                    User(
                        username = "testuser_event_${System.currentTimeMillis()}",
                        password = "hashedPassword",
                    ),
                )
        }

        @Test
        fun `publishLoginEvent should create activity log entry`() =
            runBlocking {
                // When
                activityEventPublisher.publishLoginEvent(testUser.id!!)

                // Wait for async processing
                delay(100)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 1

                val activityLog = activityLogs[0]
                activityLog.userId shouldBe testUser.id
                activityLog.action shouldBe "LOGIN"
                activityLog.entityType shouldBe "USER"
                activityLog.entityId shouldBe testUser.id
                activityLog.description shouldBe "User logged in"
            }

        @Test
        fun `publishCreateNoteEvent should create activity log entry`() =
            runBlocking {
                // Given
                val noteId = 42L
                val noteTitle = "Integration Test Note"

                // When
                activityEventPublisher.publishCreateNoteEvent(testUser.id!!, noteId, noteTitle)

                // Wait for async processing
                delay(100)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 1

                val activityLog = activityLogs[0]
                activityLog.userId shouldBe testUser.id
                activityLog.action shouldBe "CREATE"
                activityLog.entityType shouldBe "NOTE"
                activityLog.entityId shouldBe noteId
                activityLog.description shouldContain noteTitle
            }

        @Test
        fun `publishUpdateNoteEvent should create activity log entry`() =
            runBlocking {
                // Given
                val noteId = 43L
                val noteTitle = "Updated Integration Test Note"

                // When
                activityEventPublisher.publishUpdateNoteEvent(testUser.id!!, noteId, noteTitle)

                // Wait for async processing
                delay(100)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 1

                val activityLog = activityLogs[0]
                activityLog.userId shouldBe testUser.id
                activityLog.action shouldBe "UPDATE"
                activityLog.entityType shouldBe "NOTE"
                activityLog.entityId shouldBe noteId
                activityLog.description shouldContain noteTitle
            }

        @Test
        fun `publishArchiveNoteEvent should create activity log entry`() =
            runBlocking {
                // Given
                val noteId = 44L
                val noteTitle = "Archived Integration Test Note"

                // When
                activityEventPublisher.publishArchiveNoteEvent(testUser.id!!, noteId, noteTitle)

                // Wait for async processing
                delay(100)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 1

                val activityLog = activityLogs[0]
                activityLog.userId shouldBe testUser.id
                activityLog.action shouldBe "ARCHIVE"
                activityLog.entityType shouldBe "NOTE"
                activityLog.entityId shouldBe noteId
                activityLog.description shouldContain noteTitle
            }

        @Test
        fun `publishShareNoteEvent should create activity log entry`() =
            runBlocking {
                // Given
                val noteId = 45L
                val noteTitle = "Shared Integration Test Note"
                val sharedWithUsername = "recipient"

                // When
                activityEventPublisher.publishShareNoteEvent(testUser.id!!, noteId, noteTitle, sharedWithUsername)

                // Wait for async processing
                delay(100)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 1

                val activityLog = activityLogs[0]
                activityLog.userId shouldBe testUser.id
                activityLog.action shouldBe "SHARE"
                activityLog.entityType shouldBe "NOTE"
                activityLog.entityId shouldBe noteId
                activityLog.description shouldContain noteTitle
                activityLog.description shouldContain sharedWithUsername
            }

        @Test
        fun `multiple events should create multiple activity log entries`() =
            runBlocking {
                // When
                activityEventPublisher.publishLoginEvent(testUser.id!!)
                activityEventPublisher.publishCreateNoteEvent(testUser.id!!, 1L, "Note 1")
                activityEventPublisher.publishUpdateNoteEvent(testUser.id!!, 1L, "Updated Note 1")

                // Wait for async processing
                delay(200)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 3

                // Verify we have all three types of activities
                val actions = activityLogs.map { it.action }.toSet()
                actions shouldBe setOf("LOGIN", "CREATE", "UPDATE")
            }

        @Test
        fun `event publishing should be resilient to async failures`() =
            runBlocking {
                // When - publish many events quickly
                repeat(5) { i ->
                    activityEventPublisher.publishCreateNoteEvent(testUser.id!!, i.toLong(), "Note $i")
                }

                // Wait for async processing
                delay(300)

                // Then
                val activityLogs = activityLogRepository.findAll()
                activityLogs.size shouldBe 5

                // All should be CREATE events
                activityLogs.all { it.action == "CREATE" } shouldBe true
            }
    }
