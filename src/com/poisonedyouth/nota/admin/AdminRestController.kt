package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminRestController(
    private val adminService: AdminService,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    private fun ensureAdmin(session: HttpSession): ResponseEntity<*>? {
        val currentUser =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))
        val isAdmin = adminService.isAdmin(currentUser.username)
        return if (isAdmin) {
            null
        } else {
            ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Admin access required"))
        }
    }

    @GetMapping("/system-stats")
    fun systemStats(session: HttpSession): ResponseEntity<*> {
        ensureAdmin(session)?.let { return it }
        val stats = adminService.getSystemStatistics()
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/users-stats")
    fun usersStats(session: HttpSession): ResponseEntity<*> {
        ensureAdmin(session)?.let { return it }
        val stats = adminService.getAllUserStatistics()
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/users/{userId}/disable")
    fun disableUser(
        @PathVariable userId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        ensureAdmin(session)?.let { return it }
        val success = adminService.disableUser(userId)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "User has been disabled successfully"))
        } else {
            ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Failed to disable user. Admin users cannot be disabled."))
        }
    }

    @PostMapping("/users/{userId}/enable")
    fun enableUser(
        @PathVariable userId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        ensureAdmin(session)?.let { return it }
        val success = adminService.enableUser(userId)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "User has been enabled successfully"))
        } else {
            ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Failed to enable user"))
        }
    }
}
