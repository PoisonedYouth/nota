package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/notes/{noteId}/attachments")
class NoteAttachmentRestController(
    private val noteAttachmentService: NoteAttachmentService,
    private val noteService: NoteService,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    @PostMapping
    fun uploadAttachment(
        @PathVariable noteId: Long,
        @RequestParam("file") file: MultipartFile,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        return try {
            val dto = noteAttachmentService.addAttachment(noteId, file, user.id)
            ResponseEntity.status(HttpStatus.CREATED).body(dto)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
        }
    }

    @GetMapping
    fun listAttachments(
        @PathVariable noteId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val attachments = noteAttachmentService.listAttachments(noteId, user.id)
        return ResponseEntity.ok(mapOf("attachments" to attachments, "count" to attachments.size))
    }

    @GetMapping("/{attachmentId}/download")
    fun downloadAttachment(
        @PathVariable noteId: Long,
        @PathVariable attachmentId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val attachment = noteAttachmentService.getAttachment(noteId, attachmentId, user.id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(attachment.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE)
        headers.contentDisposition = ContentDisposition.attachment().filename(attachment.filename).build()
        headers.set(HttpHeaders.CONTENT_LENGTH, attachment.fileSize.toString())

        return ResponseEntity.ok().headers(headers).body(attachment.data)
    }

    @DeleteMapping("/{attachmentId}")
    fun deleteAttachment(
        @PathVariable noteId: Long,
        @PathVariable attachmentId: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val success = noteAttachmentService.deleteAttachment(noteId, attachmentId, user.id)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Attachment deleted successfully"))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }
}
