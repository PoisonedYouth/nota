package com.poisonedyouth.nota.notes

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
) {

    private fun getCurrentUser(session: HttpSession): UserDto? {
        return session.getAttribute("currentUser") as? UserDto
    }

    private fun requireAuthentication(session: HttpSession): UserDto? {
        return getCurrentUser(session)
    }

    @GetMapping
    fun listNotes(model: Model, session: HttpSession): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        model.addAttribute("notes", noteService.findAllNotes(user.id))
        model.addAttribute("currentUser", user)
        return "notes/list"
    }

    @GetMapping("/{id}")
    fun showNoteDetail(
        @PathVariable id: Long,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val note = noteService.findNoteById(id, user.id)
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

        return if (htmxRequest != null) {
            // HTMX Request: Nur die neue Notiz als Fragment zurückgeben
            model.addAttribute("note", newNote)
            "notes/fragments :: note-card"
        } else {
            // Normale Request: Redirect zur Liste
            "redirect:/notes"
        }
    }

    @GetMapping("/modal/new")
    fun showCreateModal(): String {
        return "notes/create-modal :: modal-content"
    }

    @GetMapping("/count")
    fun getNotesCount(model: Model, session: HttpSession): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val notes = noteService.findAllNotes(user.id)
        model.addAttribute("notes", notes)
        return "notes/list :: .notes-count"
    }

    @DeleteMapping("/{id}")
    fun archiveNote(
        @PathVariable id: Long,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
        model: Model,
        session: HttpSession,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        noteService.archiveNote(id, user.id)

        return if (htmxRequest != null) {
            // HTMX Request: Return archive response with OOB swap to update notes count
            val notes = noteService.findAllNotes(user.id)
            model.addAttribute("notes", notes)
            "notes/fragments :: archive-response"
        } else {
            // Normal Request: Redirect to the list
            "redirect:/notes"
        }
    }

    @GetMapping("/archive")
    fun listArchivedNotes(model: Model, session: HttpSession): String {
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

        val updateNoteDto = UpdateNoteDto(
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
        val note = noteService.findNoteById(id, user.id)
        if (note == null) {
            return "redirect:/notes"
        }

        val updateNoteDto = UpdateNoteDto(
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
    fun searchNotes(
        @RequestParam(value = "q", required = false, defaultValue = "") query: String,
        model: Model,
        session: HttpSession,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        val user = requireAuthentication(session) ?: return "redirect:/auth/login"
        val notes = noteService.searchNotes(query, user.id)
        model.addAttribute("notes", notes)
        model.addAttribute("searchQuery", query)

        return if (htmxRequest != null) {
            // HTMX Request: Return only the notes grid
            "notes/list :: #notes-container"
        } else {
            // Normal Request: Return full page
            "notes/list"
        }
    }
}
