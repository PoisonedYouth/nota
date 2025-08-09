package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notes")
class NoteSharingRestController(
    private val noteService: NoteService,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    @PostMapping("/{id}/share")
    fun shareNote(
        @PathVariable id: Long,
        @RequestBody shareNoteDto: ShareNoteDto,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        if (shareNoteDto.username.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username is required"))
        }
        return try {
            val success = noteService.shareNote(id, shareNoteDto, user.id)
            if (success) {
                ResponseEntity.ok(mapOf("message" to "Note shared successfully"))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build<Any>()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}/shares/{userId}")
    fun revokeShare(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        val success = noteService.revokeNoteShare(id, userId, user.id)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Share revoked successfully"))
        } else {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/{id}/shares")
    fun listShares(
        @PathVariable id: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user = getCurrentUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Authentication required"))

        val shares = noteService.getNoteShares(id, user.id)
        return ResponseEntity.ok(mapOf("shares" to shares, "count" to shares.size))
    }
}
