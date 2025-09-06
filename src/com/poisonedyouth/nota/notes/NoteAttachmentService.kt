package com.poisonedyouth.nota.notes

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class NoteAttachmentService(
    private val noteAttachmentRepository: NoteAttachmentRepository,
    private val noteRepository: NoteRepository,
    private val noteService: NoteService,
    private val fileUploadSafetyValidator: FileUploadSafetyValidator,
) {
    fun addAttachment(noteId: Long, file: MultipartFile, userId: Long): NoteAttachmentDto {
        if (!noteService.canUserAccessNote(noteId, userId)) {
            throw IllegalArgumentException("User not allowed to access note")
        }

        // Validate file safety (size, extension, content type by magic)
        fileUploadSafetyValidator.validate(file)
        val sanitizedName = fileUploadSafetyValidator.sanitizeFilename(file.originalFilename)

        val note = noteRepository.findById(noteId).orElseThrow { IllegalArgumentException("Note not found") }
        val entity = NoteAttachment(
            note = note,
            filename = sanitizedName,
            contentType = file.contentType,
            fileSize = file.size,
            data = file.bytes,
        )
        val saved = noteAttachmentRepository.save(entity)
        return NoteAttachmentDto.fromEntity(saved)
    }

    fun listAttachments(noteId: Long, userId: Long): List<NoteAttachmentDto> {
        if (!noteService.canUserAccessNote(noteId, userId)) {
            return emptyList()
        }
        return noteAttachmentRepository.findAllByNoteIdOrderByCreatedAtDesc(noteId).map { NoteAttachmentDto.fromEntity(it) }
    }

    fun getAttachment(noteId: Long, attachmentId: Long, userId: Long): NoteAttachment? {
        if (!noteService.canUserAccessNote(noteId, userId)) {
            return null
        }
        return noteAttachmentRepository.findByIdAndNoteId(attachmentId, noteId)
    }

    fun deleteAttachment(noteId: Long, attachmentId: Long, userId: Long): Boolean {
        if (!noteService.canUserAccessNote(noteId, userId)) {
            return false
        }
        val existing = noteAttachmentRepository.findByIdAndNoteId(attachmentId, noteId) ?: return false
        noteAttachmentRepository.delete(existing)
        return true
    }
}

data class NoteAttachmentDto(
    val id: Long,
    val filename: String,
    val contentType: String?,
    val fileSize: Long,
    val createdAt: java.time.LocalDateTime,
    val version: Long,
) {
    companion object {
        fun fromEntity(entity: NoteAttachment): NoteAttachmentDto =
            NoteAttachmentDto(
                id = entity.id ?: -1L,
                filename = entity.filename,
                contentType = entity.contentType,
                fileSize = entity.fileSize,
                createdAt = entity.createdAt,
                version = entity.version,
            )
    }
}
