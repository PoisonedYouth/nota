package com.poisonedyouth.nota.activitylog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "activity_log")
data class ActivityLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "action", nullable = false, length = 50)
    val action: String,
    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,
    @Column(name = "entity_id")
    val entityId: Long? = null,
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
