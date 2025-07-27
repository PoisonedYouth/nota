package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Suppress("TooManyFunctions") // Repository with various query methods
interface NoteRepository : JpaRepository<Note, Long> {
    // User-filtered methods
    fun findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(user: User): List<Note>
    fun findAllByUserAndArchivedTrueOrderByUpdatedAtDesc(user: User): List<Note>
    fun findByIdAndUser(id: Long, user: User): Note?

    // Count methods for admin statistics
    fun countByUser(user: User): Long
    fun countByUserAndArchivedTrue(user: User): Long
    fun countByArchivedTrue(): Long

    // User-filtered search methods
    fun findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
        user: User,
        title: String,
        content: String,
    ): List<Note>

    // Dynamic sorting methods using Spring Data Sort
    fun findAllByUserAndArchivedFalse(user: User, sort: Sort): List<Note>

    // Dynamic sorting search methods
    @Query(
        """
        SELECT n FROM Note n
        WHERE n.user = :user AND n.archived = false
        AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))
    """,
    )
    fun findAllByUserAndArchivedFalseAndQuery(
        @Param("user") user: User,
        @Param("query") query: String,
        sort: Sort,
    ): List<Note>

    // Methods that consider shared notes
    @Query(
        """
        SELECT DISTINCT n FROM Note n
        LEFT JOIN NoteShare ns ON n.id = ns.note.id
        WHERE (n.user = :user OR ns.sharedWithUser = :user)
        AND n.archived = false
    """,
    )
    fun findAllAccessibleByUserAndArchivedFalse(
        @Param("user") user: User,
        sort: Sort,
    ): List<Note>

    @Query(
        """
        SELECT n FROM Note n
        LEFT JOIN NoteShare ns ON n.id = ns.note.id
        WHERE (n.user = :user OR ns.sharedWithUser = :user)
        AND n.id = :id
    """,
    )
    fun findByIdAndAccessibleByUser(
        @Param("id") id: Long,
        @Param("user") user: User,
    ): Note?

    @Query(
        """
        SELECT DISTINCT n FROM Note n
        LEFT JOIN NoteShare ns ON n.id = ns.note.id
        WHERE (n.user = :user OR ns.sharedWithUser = :user)
        AND n.archived = false
        AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))
    """,
    )
    fun findAllAccessibleByUserAndArchivedFalseAndQuery(
        @Param("user") user: User,
        @Param("query") query: String,
        sort: Sort,
    ): List<Note>
}
