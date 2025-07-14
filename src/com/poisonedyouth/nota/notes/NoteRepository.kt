package com.poisonedyouth.nota.notes

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteRepository : JpaRepository<Note, Long> {
    fun findAllByOrderByUpdatedAtDesc(): List<Note>
    fun findAllByArchivedFalseOrderByUpdatedAtDesc(): List<Note>
    fun findAllByArchivedTrueOrderByUpdatedAtDesc(): List<Note>

    // Search methods
    fun findAllByArchivedFalseAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(title: String): List<Note>
    fun findAllByArchivedFalseAndContentContainingIgnoreCaseOrderByUpdatedAtDesc(content: String): List<Note>
    fun findAllByArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
        title: String,
        content: String,
    ): List<Note>
}
