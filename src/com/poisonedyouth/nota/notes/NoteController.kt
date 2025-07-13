package com.poisonedyouth.nota.notes

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/notes")
class NoteController(
    private val noteService: NoteService,
) {

    @GetMapping
    fun listNotes(model: Model): String {
        model.addAttribute("notes", noteService.findAllNotes())
        return "notes/list"
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
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        if (bindingResult.hasErrors()) {
            return if (htmxRequest != null) {
                model.addAttribute("createNoteDto", createNoteDto)
                "notes/create-modal :: modal-content"
            } else {
                "notes/create-modal"
            }
        }

        val newNote = noteService.createNote(createNoteDto)

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
    fun getNotesCount(model: Model): String {
        val notes = noteService.findAllNotes()
        model.addAttribute("notes", notes)
        return "notes/list :: .notes-count"
    }

    @DeleteMapping("/{id}")
    fun archiveNote(
        @PathVariable id: Long,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
        model: Model,
    ): String {
        noteService.archiveNote(id)

        return if (htmxRequest != null) {
            // HTMX Request: Return archive response with OOB swap to update notes count
            val notes = noteService.findAllNotes()
            model.addAttribute("notes", notes)
            "notes/fragments :: archive-response"
        } else {
            // Normal Request: Redirect to the list
            "redirect:/notes"
        }
    }

    @GetMapping("/archive")
    fun listArchivedNotes(model: Model): String {
        model.addAttribute("notes", noteService.findAllArchivedNotes())
        return "notes/archive"
    }
}
