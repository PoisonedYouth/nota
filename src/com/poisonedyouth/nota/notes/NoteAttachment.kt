package com.poisonedyouth.nota.notes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "note_attachments")
@Suppress("LongParameterList") // JPA entity requires all these parameters for proper mapping
class NoteAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    val note: Note,

    @Column(nullable = false)
    val filename: String,

    @Column(name = "content_type", nullable = true)
    val contentType: String? = null,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Lob
    @Column(nullable = false)
    val data: ByteArray,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
