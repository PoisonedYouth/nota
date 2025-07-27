package com.poisonedyouth.nota.admin

/**
 * DTO representing overall system statistics for admin overview
 */
data class AdminSystemStatisticsDto(
    val totalUsers: Long,
    val totalNotes: Long,
    val totalArchivedNotes: Long,
    val totalSharedNotes: Long,
)
