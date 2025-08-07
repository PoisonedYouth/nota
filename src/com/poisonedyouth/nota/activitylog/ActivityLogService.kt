package com.poisonedyouth.nota.activitylog

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ActivityLogService(
    private val activityLogRepository: ActivityLogRepository,
) {
    fun logActivity(
        userId: Long,
        action: String,
        entityType: String,
        entityId: Long? = null,
        description: String,
    ) {
        val activityLog =
            ActivityLog(
                userId = userId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                description = description,
            )
        activityLogRepository.save(activityLog)
    }

    fun getRecentActivities(
        userId: Long,
        limit: Int = 20,
    ): List<ActivityLogDto> {
        val pageable = PageRequest.of(0, limit)
        return activityLogRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .content
            .map { ActivityLogDto.fromEntity(it) }
    }

    fun getActivitiesPage(
        userId: Long,
        page: Int = 0,
        size: Int = 20,
    ): Page<ActivityLogDto> {
        val pageable = PageRequest.of(page, size)
        val activityPage = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        return activityPage.map { ActivityLogDto.fromEntity(it) }
    }

    fun getAllActivities(userId: Long): List<ActivityLogDto> =
        activityLogRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .map { ActivityLogDto.fromEntity(it) }
}
