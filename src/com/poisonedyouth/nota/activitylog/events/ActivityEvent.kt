package com.poisonedyouth.nota.activitylog.events

/**
 * Base class for all activity events
 */
abstract class ActivityEvent(
    val userId: Long,
    val action: String,
    val entityType: String,
    val entityId: Long?,
    val description: String,
)
