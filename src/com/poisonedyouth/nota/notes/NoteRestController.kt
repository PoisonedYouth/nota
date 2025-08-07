package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notes")
class NoteRestController(
    private val noteService: NoteService,
    private val activityEventPublisher: ActivityEventPublisher,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    @PostMapping
    fun createNote(
        @RequestBody createNoteDto: CreateNoteDto,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        return try {
            val newNote = noteService.createNote(createNoteDto, user.id)

            // Publish create note event
            activityEventPublisher.publishCreateNoteEvent(user.id, newNote.id, newNote.title)

            ResponseEntity.status(HttpStatus.CREATED).body(newNote)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    fun listNotes(
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val notes = noteService.findAllNotes(user.id, sortBy, sortOrder)
        return ResponseEntity.ok(
            mapOf(
                "notes" to notes,
                "count" to notes.size,
            ),
        )
    }

    @GetMapping("/{id}")
    fun getNoteById(
        @PathVariable id: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val note = noteService.findAccessibleNoteById(id, user.id)
        return if (note != null) {
            ResponseEntity.ok(note)
        } else {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @PutMapping("/{id}")
    fun updateNote(
        @PathVariable id: Long,
        @RequestBody updateNoteDto: UpdateNoteDto,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        return try {
            val updatedNote = noteService.updateNote(updateNoteDto, user.id)
            if (updatedNote != null) {
                // Publish update note event
                activityEventPublisher.publishUpdateNoteEvent(user.id, updatedNote.id, updatedNote.title)
                ResponseEntity.ok(updatedNote)
            } else {
                ResponseEntity.notFound().build<Any>()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun archiveNote(
        @PathVariable id: Long,
        session: HttpSession,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val note = noteService.findAccessibleNoteById(id, user.id)
        val success = noteService.archiveNote(id, user.id)

        return if (success) {
            // Publish archive note event
            if (note != null) {
                activityEventPublisher.publishArchiveNoteEvent(user.id, id, note.title)
            }
            ResponseEntity.ok(mapOf("message" to "Note archived successfully"))
        } else {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @GetMapping("/search")
    fun searchNotes(
        @RequestParam(value = "q", required = false, defaultValue = "") query: String,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
        @RequestParam(value = "all", required = false, defaultValue = "false") searchAll: String,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val notes =
            if (searchAll == "true") {
                noteService.searchAccessibleNotes(query, user.id, sortBy, sortOrder)
            } else {
                noteService.searchNotes(query, user.id, sortBy, sortOrder)
            }

        return ResponseEntity.ok(
            mapOf(
                "notes" to notes,
                "query" to query,
                "count" to notes.size,
            ),
        )
    }

    @GetMapping("/shared")
    fun listSharedNotes(
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val sharedNotes = noteService.findAllSharedNotes(user.id, sortBy, sortOrder)
        return ResponseEntity.ok(
            mapOf(
                "notes" to sharedNotes,
                "count" to sharedNotes.size,
            ),
        )
    }

    @GetMapping("/all")
    fun listAllAccessibleNotes(
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): ResponseEntity<*> {
        val user =
            getCurrentUser(session)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val allNotes = noteService.findAllAccessibleNotes(user.id, sortBy, sortOrder)
        return ResponseEntity.ok(
            mapOf(
                "notes" to allNotes,
                "count" to allNotes.size,
            ),
        )
    }
}
