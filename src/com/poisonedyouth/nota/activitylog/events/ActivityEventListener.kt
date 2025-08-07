package com.poisonedyouth.nota.activitylog.events

import com.poisonedyouth.nota.activitylog.ActivityLogService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Event listener that handles activity events and creates activity logs asynchronously
 */
@Component
class ActivityEventListener(
    private val activityLogService: ActivityLogService,
) {
    @EventListener
    @Async
    fun handleActivityEvent(event: ActivityEvent) {
        activityLogService.logActivity(
            userId = event.userId,
            action = event.action,
            entityType = event.entityType,
            entityId = event.entityId,
            description = event.description,
        )
    }
}
