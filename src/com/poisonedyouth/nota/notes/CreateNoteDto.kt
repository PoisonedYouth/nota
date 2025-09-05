package com.poisonedyouth.nota.notes

import java.time.LocalDateTime

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateNoteDto(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,
    @field:Size(max = 10000, message = "Content is too long")
    val content: String = "",
    val dueDate: LocalDateTime? = null,
)
