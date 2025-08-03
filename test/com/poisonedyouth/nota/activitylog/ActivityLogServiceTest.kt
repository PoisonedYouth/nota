package com.poisonedyouth.nota.activitylog

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class ActivityLogServiceTest {

    private val activityLogRepository = mockk<ActivityLogRepository>()
    private val activityLogService = ActivityLogService(activityLogRepository)

    @Test
    fun `logActivity should save activity log`() {
        // Given
        val userId = 1L
        val action = "CREATE"
        val entityType = "NOTE"
        val entityId = 123L
        val description = "Test activity"

        every { activityLogRepository.save(any()) } returns mockk()

        // When
        activityLogService.logActivity(userId, action, entityType, entityId, description)

        // Then
        verify {
            activityLogRepository.save(
                match { activityLog ->
                    activityLog.userId == userId &&
                        activityLog.action == action &&
                        activityLog.entityType == entityType &&
                        activityLog.entityId == entityId &&
                        activityLog.description == description
                },
            )
        }
    }

    @Test
    fun `getRecentActivities should return limited activities`() {
        // Given
        val userId = 1L
        val limit = 10
        val activities = listOf(
            ActivityLog(
                id = 1L,
                userId = userId,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 123L,
                description = "Created note",
                createdAt = LocalDateTime.now(),
            ),
            ActivityLog(
                id = 2L,
                userId = userId,
                action = "UPDATE",
                entityType = "NOTE",
                entityId = 123L,
                description = "Updated note",
                createdAt = LocalDateTime.now().minusMinutes(5),
            ),
        )

        val pageable = PageRequest.of(0, limit)
        val activitiesPage = PageImpl(activities, pageable, activities.size.toLong())

        every {
            activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        } returns activitiesPage

        // When
        val result = activityLogService.getRecentActivities(userId, limit)

        // Then
        result.size shouldBe 2
        result[0].action shouldBe "CREATE"
        result[0].description shouldBe "Created note"
        result[1].action shouldBe "UPDATE"
        result[1].description shouldBe "Updated note"
    }

    @Test
    fun `getActivitiesPage should return paginated activities`() {
        // Given
        val userId = 1L
        val page = 1
        val size = 5
        val activities = listOf(
            ActivityLog(
                id = 1L,
                userId = userId,
                action = "CREATE",
                entityType = "NOTE",
                entityId = 123L,
                description = "Created note",
                createdAt = LocalDateTime.now(),
            ),
            ActivityLog(
                id = 2L,
                userId = userId,
                action = "UPDATE",
                entityType = "NOTE",
                entityId = 123L,
                description = "Updated note",
                createdAt = LocalDateTime.now().minusMinutes(5),
            ),
        )

        val pageable = PageRequest.of(page, size)
        val activitiesPage = PageImpl(activities, pageable, 10L) // Total of 10 activities

        every {
            activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        } returns activitiesPage

        // When
        val result = activityLogService.getActivitiesPage(userId, page, size)

        // Then
        result.content.size shouldBe 2
        result.totalElements shouldBe 10L
        result.totalPages shouldBe 2
        result.number shouldBe 1
        result.size shouldBe 5
        result.hasNext() shouldBe false
        result.hasPrevious() shouldBe true
        result.content[0].action shouldBe "CREATE"
        result.content[0].description shouldBe "Created note"
        result.content[1].action shouldBe "UPDATE"
        result.content[1].description shouldBe "Updated note"
    }

    @Test
    fun `getAllActivities should return all activities for user`() {
        // Given
        val userId = 1L
        val activities = listOf(
            ActivityLog(
                id = 1L,
                userId = userId,
                action = "LOGIN",
                entityType = "USER",
                entityId = userId,
                description = "User logged in",
                createdAt = LocalDateTime.now(),
            ),
        )

        every { activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId) } returns activities

        // When
        val result = activityLogService.getAllActivities(userId)

        // Then
        result.size shouldBe 1
        result[0].action shouldBe "LOGIN"
        result[0].description shouldBe "User logged in"
        result[0].formattedCreatedAt shouldNotBe null
    }
}
