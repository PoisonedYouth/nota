package com.poisonedyouth.nota.common

import com.poisonedyouth.nota.notes.Note
import com.poisonedyouth.nota.notes.NoteAttachment
import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRole
import java.time.Clock
import java.time.LocalDateTime

/**
 * Builder for Note entities with validation and defaults
 */
class NoteBuilder(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private var title: String? = null
    private var content: String = ""
    private var dueDate: LocalDateTime? = null
    private var user: User? = null
    private var archived: Boolean = false
    private var archivedAt: LocalDateTime? = null

    fun title(title: String) = apply { this.title = title }
    fun content(content: String) = apply { this.content = content }
    fun dueDate(dueDate: LocalDateTime?) = apply { this.dueDate = dueDate }
    fun user(user: User) = apply { this.user = user }
    fun archived(archived: Boolean) = apply {
        this.archived = archived
        if (archived && archivedAt == null) {
            this.archivedAt = LocalDateTime.now(clock)
        } else if (!archived) {
            this.archivedAt = null
        }
    }
    fun archivedAt(archivedAt: LocalDateTime?) = apply { this.archivedAt = archivedAt }

    fun build(): Note {
        requireNotNull(title) { "Note title is required" }
        requireNotNull(user) { "Note user is required" }
        require(title!!.isNotBlank()) { "Note title cannot be blank" }

        val now = LocalDateTime.now(clock)
        return Note(
            title = title!!,
            content = content,
            dueDate = dueDate,
            user = user!!,
            archived = archived,
            archivedAt = archivedAt,
            createdAt = now,
            updatedAt = now,
        )
    }
}

/**
 * Builder for User entities with validation and defaults
 */
class UserBuilder {
    private var username: String? = null
    private var password: String? = null
    private var mustChangePassword: Boolean = false
    private var role: UserRole = UserRole.USER
    private var enabled: Boolean = true

    fun username(username: String) = apply { this.username = username }
    fun password(password: String) = apply { this.password = password }
    fun mustChangePassword(mustChangePassword: Boolean) = apply { this.mustChangePassword = mustChangePassword }
    fun role(role: UserRole) = apply { this.role = role }
    fun enabled(enabled: Boolean) = apply { this.enabled = enabled }

    fun build(): User {
        requireNotNull(username) { "Username is required" }
        requireNotNull(password) { "Password is required" }
        require(username!!.isNotBlank()) { "Username cannot be blank" }
        require(password!!.isNotBlank()) { "Password cannot be blank" }

        return User(
            username = username!!,
            password = password!!,
            mustChangePassword = mustChangePassword,
            role = role,
            enabled = enabled,
        )
    }
}

/**
 * Builder for NoteAttachment entities with validation
 */
class NoteAttachmentBuilder(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private var note: Note? = null
    private var filename: String? = null
    private var contentType: String? = null
    private var fileSize: Long? = null
    private var data: ByteArray? = null

    fun note(note: Note) = apply { this.note = note }
    fun filename(filename: String) = apply { this.filename = filename }
    fun contentType(contentType: String?) = apply { this.contentType = contentType }
    fun fileSize(fileSize: Long) = apply { this.fileSize = fileSize }
    fun data(data: ByteArray) = apply { this.data = data }

    fun build(): NoteAttachment {
        requireNotNull(note) { "Note is required for attachment" }
        requireNotNull(filename) { "Filename is required" }
        requireNotNull(fileSize) { "File size is required" }
        requireNotNull(data) { "File data is required" }
        require(filename!!.isNotBlank()) { "Filename cannot be blank" }
        require(fileSize!! > 0) { "File size must be positive" }

        return NoteAttachment(
            note = note!!,
            filename = filename!!,
            contentType = contentType,
            fileSize = fileSize!!,
            data = data!!,
            createdAt = LocalDateTime.now(clock),
        )
    }
}

/**
 * Factory object for creating builders with consistent clock configuration
 */
object EntityBuilders {

    fun noteBuilder(clock: Clock = Clock.systemDefaultZone()): NoteBuilder =
        NoteBuilder(clock)

    fun userBuilder(): UserBuilder =
        UserBuilder()

    fun attachmentBuilder(clock: Clock = Clock.systemDefaultZone()): NoteAttachmentBuilder =
        NoteAttachmentBuilder(clock)
}
