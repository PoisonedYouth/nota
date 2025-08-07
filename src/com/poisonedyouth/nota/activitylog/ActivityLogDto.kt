package com.poisonedyouth.nota.activitylog

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ActivityLogDto(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: Long?,
    val description: String,
    val createdAt: LocalDateTime,
) {
    val formattedCreatedAt: String
        get() = createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    companion object {
        fun fromEntity(activityLog: ActivityLog): ActivityLogDto =
            ActivityLogDto(
                id = activityLog.id,
                action = activityLog.action,
                entityType = activityLog.entityType,
                entityId = activityLog.entityId,
                description = activityLog.description,
                createdAt = activityLog.createdAt,
            )
    }
}
