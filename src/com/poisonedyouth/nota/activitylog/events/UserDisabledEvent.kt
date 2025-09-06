package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when an admin disables a user
 */
class UserDisabledEvent(
    adminUserId: Long,
    targetUserId: Long,
) : ActivityEvent(
    userId = adminUserId,
    action = "DISABLE",
    entityType = "USER",
    entityId = targetUserId,
    description = "User disabled: id $targetUserId",
)
