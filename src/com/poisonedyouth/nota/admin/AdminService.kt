package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.notes.NoteRepository
import com.poisonedyouth.nota.notes.NoteShareRepository
import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminService(
    private val userRepository: UserRepository,
    private val noteRepository: NoteRepository,
    private val noteShareRepository: NoteShareRepository,
) {

    /**
     * Get statistics for all users in the system
     */
    fun getAllUserStatistics(): List<AdminUserStatisticsDto> {
        val allUsers = userRepository.findAll()

        return allUsers.map { user ->
            val totalNotes = noteRepository.countByUser(user)
            val archivedNotes = noteRepository.countByUserAndArchivedTrue(user)
            val sharedNotes = noteShareRepository.countBySharedByUser(user)

            AdminUserStatisticsDto.fromUser(
                user = user,
                totalNotes = totalNotes,
                archivedNotes = archivedNotes,
                sharedNotes = sharedNotes,
            )
        }
    }

    /**
     * Check if the current user is an admin based on their role
     */
    fun isAdmin(username: String): Boolean {
        val user = userRepository.findByUsername(username)
        return user?.role == UserRole.ADMIN
    }

    /**
     * Get overall system statistics
     */
    fun getSystemStatistics(): AdminSystemStatisticsDto {
        val totalUsers = userRepository.count()
        val totalNotes = noteRepository.count()
        val totalArchivedNotes = noteRepository.countByArchivedTrue()
        val totalSharedNotes = noteShareRepository.count()

        return AdminSystemStatisticsDto(
            totalUsers = totalUsers,
            totalNotes = totalNotes,
            totalArchivedNotes = totalArchivedNotes,
            totalSharedNotes = totalSharedNotes,
        )
    }

    /**
     * Disable a user by setting enabled to false
     */
    @Transactional
    fun disableUser(userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // Don't allow disabling admin users
        if (user.role == UserRole.ADMIN) {
            return false
        }

        val updatedUser = User(
            id = user.id,
            username = user.username,
            password = user.password,
            mustChangePassword = user.mustChangePassword,
            role = user.role,
            createdAt = user.createdAt,
            updatedAt = LocalDateTime.now(),
            enabled = false,
        )
        userRepository.save(updatedUser)
        return true
    }

    /**
     * Enable a user by setting enabled to true
     */
    @Transactional
    fun enableUser(userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        val updatedUser = User(
            id = user.id,
            username = user.username,
            password = user.password,
            mustChangePassword = user.mustChangePassword,
            role = user.role,
            createdAt = user.createdAt,
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )
        userRepository.save(updatedUser)
        return true
    }
}
