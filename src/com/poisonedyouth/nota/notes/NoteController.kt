package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.activitylog.ActivityLogService
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/notes")
@Suppress("TooManyFunctions")
class NoteController(
    private val noteService: NoteService,
    private val activityLogService: ActivityLogService,
    private val activityEventPublisher: ActivityEventPublisher,
) {
    private fun getCurrentUser(session: HttpSession): UserDto? = session.getAttribute("currentUser") as? UserDto

    private fun requireAuthentication(session: HttpSession): UserDto? = getCurrentUser(session)

    @GetMapping
    fun listNotes(
        model: Model,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        model.addAttribute("notes", noteService.findAllNotes(user.id, sortBy, sortOrder))
        model.addAttribute("currentUser", user)
        model.addAttribute("currentSort", sortBy)
        model.addAttribute("currentOrder", sortOrder)
        return "notes/list"
    }

    @GetMapping("/{id}")
    fun showNoteDetail(
        @PathVariable id: Long,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findAccessibleNoteById(id, user.id)
        if (note == null) {
            return "redirect:/notes"
        }

        model.addAttribute("note", note)
        model.addAttribute("currentUser", user)
        return "notes/detail"
    }

    @GetMapping("/new")
    fun showCreateForm(model: Model): String {
        model.addAttribute("createNoteDto", CreateNoteDto("", ""))
        return "notes/create-modal"
    }

    @PostMapping("/new")
    fun createNote(
        @ModelAttribute createNoteDto: CreateNoteDto,
        bindingResult: BindingResult,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"

        if (bindingResult.hasErrors()) {
            return if (htmxRequest != null) {
                model.addAttribute("createNoteDto", createNoteDto)
                "notes/create-modal :: modal-content"
            } else {
                "notes/create-modal"
            }
        }

        val newNote = noteService.createNote(createNoteDto, user.id)

        // Publish create note event
        activityEventPublisher.publishCreateNoteEvent(user.id, newNote.id, newNote.title)

        return if (htmxRequest != null) {
            // HTMX Request: Return only the new note as fragment
            model.addAttribute("note", newNote)
            "notes/fragments :: note-card"
        } else {
            // Normal Request: Redirect to list
            "redirect:/notes"
        }
    }

    @GetMapping("/modal/new")
    fun showCreateModal(): String = "notes/create-modal :: modal-content"

    @GetMapping("/modal/{id}")
    fun showUnifiedModal(
        @PathVariable id: Long,
        @RequestParam(value = "mode", required = false, defaultValue = "edit") mode: String,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findAccessibleNoteById(id, user.id)
        if (note == null) {
            return "redirect:/notes"
        }

        model.addAttribute("note", note)
        model.addAttribute("mode", mode)

        if (mode == "share") {
            val shares = noteService.getNoteShares(id, user.id)
            model.addAttribute("shares", shares)
            model.addAttribute("shareNoteDto", ShareNoteDto(""))
        }

        return "notes/create-modal :: modal-content"
    }

    @GetMapping("/count")
    fun getNotesCount(
        model: Model,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val notes = noteService.findAllNotes(user.id, sortBy, sortOrder)
        model.addAttribute("notes", notes)
        return "notes/list :: .notes-count"
    }

    @DeleteMapping("/{id}")
    @Suppress("LongParameterList")
    fun archiveNote(
        @PathVariable id: Long,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
        model: Model,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findAccessibleNoteById(id, user.id)
        noteService.archiveNote(id, user.id)

        // Publish archive note event
        if (note != null) {
            activityEventPublisher.publishArchiveNoteEvent(user.id, id, note.title)
        }

        return if (htmxRequest != null) {
            // HTMX Request: Return archive response with OOB swap to update notes count
            val notes = noteService.findAllNotes(user.id, sortBy, sortOrder)
            model.addAttribute("notes", notes)
            "notes/fragments :: archive-response"
        } else {
            // Normal Request: Redirect to the list
            "redirect:/notes"
        }
    }

    @GetMapping("/archive")
    fun listArchivedNotes(
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        model.addAttribute("notes", noteService.findAllArchivedNotes(user.id))
        return "notes/archive"
    }

    @GetMapping("/{id}/edit")
    fun showEditForm(
        @PathVariable id: Long,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findNoteById(id, user.id)
        if (note == null) {
            return "redirect:/notes"
        }

        val updateNoteDto =
            UpdateNoteDto(
                id = note.id,
                title = note.title,
                content = note.content,
                dueDate = note.dueDate,
            )

        model.addAttribute("updateNoteDto", updateNoteDto)
        model.addAttribute("note", note)
        return "notes/edit-modal"
    }

    @GetMapping("/modal/{id}/edit")
    fun showEditModal(
        @PathVariable id: Long,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findAccessibleNoteById(id, user.id)
        if (note == null) {
            return "redirect:/notes"
        }

        val updateNoteDto =
            UpdateNoteDto(
                id = note.id,
                title = note.title,
                content = note.content,
                dueDate = note.dueDate,
            )

        model.addAttribute("updateNoteDto", updateNoteDto)
        model.addAttribute("note", note)
        return "notes/edit-modal :: modal-content"
    }

    @PutMapping("/{id}")
    @Suppress("LongParameterList")
    fun updateNote(
        @PathVariable id: Long,
        @ModelAttribute updateNoteDto: UpdateNoteDto,
        bindingResult: BindingResult,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"

        if (bindingResult.hasErrors()) {
            val note = noteService.findNoteById(id, user.id)
            return if (htmxRequest != null) {
                model.addAttribute("updateNoteDto", updateNoteDto)
                model.addAttribute("note", note)
                "notes/edit-modal :: modal-content"
            } else {
                "notes/edit-modal"
            }
        }

        val updatedNote = noteService.updateNote(updateNoteDto, user.id)
        if (updatedNote == null) {
            return "redirect:/notes"
        }

        // Publish update note event
        activityEventPublisher.publishUpdateNoteEvent(user.id, updatedNote.id, updatedNote.title)

        return if (htmxRequest != null) {
            // HTMX Request: Return updated note card
            model.addAttribute("note", updatedNote)
            "notes/fragments :: note-card"
        } else {
            // Normal Request: Redirect to the list
            "redirect:/notes"
        }
    }

    @GetMapping("/search")
    @Suppress("LongParameterList")
    fun searchNotes(
        @RequestParam(value = "q", required = false, defaultValue = "") query: String,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
        @RequestParam(value = "all", required = false, defaultValue = "false") searchAll: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val notes =
            if (searchAll == "true") {
                noteService.searchAccessibleNotes(query, user.id, sortBy, sortOrder)
            } else {
                noteService.searchNotes(query, user.id, sortBy, sortOrder)
            }
        model.addAttribute("notes", notes)
        model.addAttribute("searchQuery", query)
        model.addAttribute("currentSort", sortBy)
        model.addAttribute("currentOrder", sortOrder)
        model.addAttribute("currentUser", user)

        return if (htmxRequest != null) {
            // HTMX Request: Return only the notes grid
            if (searchAll == "true") {
                "notes/all :: #notes-container"
            } else {
                "notes/list :: #notes-container"
            }
        } else {
            // Normal Request: Return full page
            if (searchAll == "true") {
                "notes/all"
            } else {
                "notes/list"
            }
        }
    }

    // Sharing endpoints

    @PostMapping("/{id}/share")
    @Suppress("LongParameterList")
    fun shareNote(
        @PathVariable id: Long,
        @ModelAttribute shareNoteDto: ShareNoteDto,
        bindingResult: BindingResult,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"

        if (bindingResult.hasErrors() || shareNoteDto.username.isBlank()) {
            val note = noteService.findNoteById(id, user.id)
            val shares = noteService.getNoteShares(id, user.id)
            model.addAttribute("note", note)
            model.addAttribute("shares", shares)
            model.addAttribute("shareNoteDto", shareNoteDto)
            model.addAttribute("mode", "share")
            model.addAttribute("error", "Please enter a valid username")
            return if (htmxRequest != null) {
                "notes/create-modal :: modal-content"
            } else {
                "redirect:/notes"
            }
        }

        val success = noteService.shareNote(id, shareNoteDto, user.id)
        if (!success) {
            val note = noteService.findNoteById(id, user.id)
            val shares = noteService.getNoteShares(id, user.id)
            model.addAttribute("note", note)
            model.addAttribute("shares", shares)
            model.addAttribute("shareNoteDto", shareNoteDto)
            model.addAttribute("mode", "share")
            model.addAttribute("error", "Note could not be shared. User not found or note already shared.")
            return if (htmxRequest != null) {
                "notes/create-modal :: modal-content"
            } else {
                "redirect:/notes"
            }
        }

        // Publish share note event
        val note = noteService.findNoteById(id, user.id)
        if (note != null) {
            activityEventPublisher.publishShareNoteEvent(user.id, id, note.title, shareNoteDto.username)
        }

        return if (htmxRequest != null) {
            val note = noteService.findNoteById(id, user.id)
            val shares = noteService.getNoteShares(id, user.id)
            model.addAttribute("note", note)
            model.addAttribute("shares", shares)
            model.addAttribute("shareNoteDto", ShareNoteDto(""))
            model.addAttribute("mode", "share")
            model.addAttribute("success", "Note shared successfully!")
            "notes/create-modal :: modal-content"
        } else {
            "redirect:/notes"
        }
    }

    @DeleteMapping("/{id}/share/{userId}")
    fun revokeShare(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"

        noteService.revokeNoteShare(id, userId, user.id)

        return if (htmxRequest != null) {
            val note = noteService.findNoteById(id, user.id)
            val shares = noteService.getNoteShares(id, user.id)
            model.addAttribute("note", note)
            model.addAttribute("shares", shares)
            model.addAttribute("shareNoteDto", ShareNoteDto(""))
            model.addAttribute("mode", "share")
            "notes/create-modal :: modal-content"
        } else {
            "redirect:/notes"
        }
    }

    @GetMapping("/shared")
    fun listSharedNotes(
        model: Model,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val sharedNotes = noteService.findAllSharedNotes(user.id, sortBy, sortOrder)
        model.addAttribute("notes", sharedNotes)
        model.addAttribute("currentSort", sortBy)
        model.addAttribute("currentOrder", sortOrder)
        model.addAttribute("isSharedView", true)
        return "notes/shared"
    }

    @GetMapping("/all")
    fun listAllAccessibleNotes(
        model: Model,
        session: HttpSession,
        @RequestParam(value = "sort", required = false, defaultValue = "updatedAt") sortBy: String,
        @RequestParam(value = "order", required = false, defaultValue = "desc") sortOrder: String,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val allNotes = noteService.findAllAccessibleNotes(user.id, sortBy, sortOrder)
        model.addAttribute("notes", allNotes)
        model.addAttribute("currentUser", user)
        model.addAttribute("currentSort", sortBy)
        model.addAttribute("currentOrder", sortOrder)
        model.addAttribute("isAllView", true)
        return "notes/all"
    }

    @GetMapping("/activity-log")
    fun showActivityLog(
        model: Model,
        session: HttpSession,
        @RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(value = "size", required = false, defaultValue = "20") size: Int,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val activitiesPage = activityLogService.getActivitiesPage(user.id, page, size)
        model.addAttribute("activities", activitiesPage.content)
        model.addAttribute("currentUser", user)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", activitiesPage.totalPages)
        model.addAttribute("totalElements", activitiesPage.totalElements)
        model.addAttribute("hasNext", activitiesPage.hasNext())
        model.addAttribute("hasPrevious", activitiesPage.hasPrevious())
        model.addAttribute("pageSize", size)
        return "notes/activity-log"
    }
}
