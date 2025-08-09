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

    // General user activities (LOGIN, etc.) - activities that are not related to specific notes
    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND a.entityType != 'NOTE' AND a.entityType != 'ATTACHMENT' ORDER BY a.createdAt DESC",
    )
    fun findGeneralUserActivitiesByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ActivityLog>

    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND a.entityType != 'NOTE' AND a.entityType != 'ATTACHMENT' ORDER BY a.createdAt DESC",
    )
    fun findGeneralUserActivitiesByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
    ): List<ActivityLog>

    // Note-related activities for a specific note
    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND ((a.entityType = 'NOTE' AND a.entityId = :noteId) OR (a.entityType = 'ATTACHMENT' AND a.entityId IN (SELECT att.id FROM NoteAttachment att WHERE att.note.id = :noteId))) ORDER BY a.createdAt DESC",
    )
    fun findNoteActivitiesByUserIdAndNoteIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        @Param("noteId") noteId: Long,
        pageable: Pageable,
    ): Page<ActivityLog>

    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND ((a.entityType = 'NOTE' AND a.entityId = :noteId) OR (a.entityType = 'ATTACHMENT' AND a.entityId IN (SELECT att.id FROM NoteAttachment att WHERE att.note.id = :noteId))) ORDER BY a.createdAt DESC",
    )
    fun findNoteActivitiesByUserIdAndNoteIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        @Param("noteId") noteId: Long,
    ): List<ActivityLog>

    // All note-related activities for a user (across all their notes)
    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND (a.entityType = 'NOTE' OR a.entityType = 'ATTACHMENT') ORDER BY a.createdAt DESC",
    )
    fun findAllNoteActivitiesByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ActivityLog>

    @Query(
        "SELECT a FROM ActivityLog a WHERE a.userId = :userId AND (a.entityType = 'NOTE' OR a.entityType = 'ATTACHMENT') ORDER BY a.createdAt DESC",
    )
    fun findAllNoteActivitiesByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
    ): List<ActivityLog>
}
