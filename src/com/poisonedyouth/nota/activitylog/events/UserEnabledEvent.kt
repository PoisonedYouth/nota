package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when an admin enables a user
 */
class UserEnabledEvent(
    adminUserId: Long,
    targetUserId: Long,
) : ActivityEvent(
    userId = adminUserId,
    action = "ENABLE",
    entityType = "USER",
    entityId = targetUserId,
    description = "User enabled: id $targetUserId",
)
