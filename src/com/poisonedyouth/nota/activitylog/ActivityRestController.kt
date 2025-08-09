package com.poisonedyouth.nota.activitylog

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/activity")
class ActivityRestController(
    private val activityLogService: ActivityLogService,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    @GetMapping
    fun getPage(
        session: HttpSession,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        val pageResult = activityLogService.getActivitiesPage(user.id, page, size)
        val response = mapOf(
            "content" to pageResult.content,
            "page" to pageResult.number,
            "size" to pageResult.size,
            "totalElements" to pageResult.totalElements,
            "totalPages" to pageResult.totalPages,
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/recent")
    fun getRecent(
        session: HttpSession,
        @RequestParam(value = "limit", defaultValue = "20") limit: Int,
    ): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        val items = activityLogService.getRecentActivities(user.id, limit)
        return ResponseEntity.ok(mapOf("activities" to items, "count" to items.size))
    }

    @GetMapping("/all")
    fun getAll(session: HttpSession): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        val items = activityLogService.getAllActivities(user.id)
        return ResponseEntity.ok(mapOf("activities" to items, "count" to items.size))
    }
}
