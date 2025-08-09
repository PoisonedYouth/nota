package com.poisonedyouth.nota.notes

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteAttachmentRepository : JpaRepository<NoteAttachment, Long> {
    fun findAllByNoteIdOrderByCreatedAtDesc(noteId: Long): List<NoteAttachment>
    fun findByIdAndNoteId(attachmentId: Long, noteId: Long): NoteAttachment?
}
