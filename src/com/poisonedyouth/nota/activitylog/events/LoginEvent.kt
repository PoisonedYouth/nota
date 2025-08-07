package com.poisonedyouth.nota.activitylog.events

/**
 * Event fired when a user logs in
 */
class LoginEvent(
    userId: Long,
) : ActivityEvent(
    userId = userId,
    action = "LOGIN",
    entityType = "USER",
    entityId = userId,
    description = "User logged in",
)
