package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteRepository : JpaRepository<Note, Long> {
    // User-filtered methods
    fun findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(user: User): List<Note>
    fun findAllByUserAndArchivedTrueOrderByUpdatedAtDesc(user: User): List<Note>
    fun findByIdAndUser(id: Long, user: User): Note?

    // User-filtered search methods
    fun findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
        user: User,
        title: String,
        content: String,
    ): List<Note>
}
