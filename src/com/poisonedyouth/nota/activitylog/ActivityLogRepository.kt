package com.poisonedyouth.nota.activitylog

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ActivityLogRepository : JpaRepository<ActivityLog, Long> {
    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ActivityLog>

    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
    ): List<ActivityLog>
}
