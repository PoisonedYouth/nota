package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NoteShareRepository : JpaRepository<NoteShare, Long> {

    // Find all shares for a specific note
    fun findAllByNote(note: Note): List<NoteShare>

    // Find all notes shared with a specific user
    fun findAllBySharedWithUser(user: User): List<NoteShare>

    // Find specific share between note and user
    fun findByNoteAndSharedWithUser(note: Note, sharedWithUser: User): NoteShare?

    // Check if a note is shared with a user
    fun existsByNoteAndSharedWithUser(note: Note, sharedWithUser: User): Boolean

    // Find all notes accessible to a user (owned or shared)
    @Query(
        """
        SELECT DISTINCT n FROM Note n
        LEFT JOIN NoteShare ns ON n.id = ns.note.id
        WHERE n.user = :user OR ns.sharedWithUser = :user
        AND n.archived = false
        ORDER BY n.updatedAt DESC
    """,
    )
    fun findAllAccessibleNotes(
        @Param("user") user: User,
    ): List<Note>

    // Find notes shared with user (not owned by user)
    @Query(
        """
        SELECT n FROM Note n
        JOIN NoteShare ns ON n.id = ns.note.id
        WHERE ns.sharedWithUser = :user
        AND n.archived = false
        ORDER BY n.updatedAt DESC
    """,
    )
    fun findAllSharedWithUser(
        @Param("user") user: User,
    ): List<Note>

    // Delete share by note and shared with user
    fun deleteByNoteAndSharedWithUser(note: Note, sharedWithUser: User)
}
