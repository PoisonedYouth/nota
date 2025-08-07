package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.User
import java.time.LocalDateTime

/**
 * DTO representing user statistics for admin overview
 */
data class AdminUserStatisticsDto(
    val id: Long,
    val username: String,
    val createdAt: LocalDateTime,
    val totalNotes: Long,
    val archivedNotes: Long,
    val sharedNotes: Long,
    val mustChangePassword: Boolean,
    val enabled: Boolean,
) {
    companion object {
        fun fromUser(
            user: User,
            totalNotes: Long,
            archivedNotes: Long,
            sharedNotes: Long,
        ): AdminUserStatisticsDto =
            AdminUserStatisticsDto(
                id = user.id!!,
                username = user.username,
                createdAt = user.createdAt,
                totalNotes = totalNotes,
                archivedNotes = archivedNotes,
                sharedNotes = sharedNotes,
                mustChangePassword = user.mustChangePassword,
                enabled = user.enabled,
            )
    }
}
