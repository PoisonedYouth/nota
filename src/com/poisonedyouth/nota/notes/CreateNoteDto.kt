package com.poisonedyouth.nota.notes

import java.time.LocalDateTime

data class CreateNoteDto(
    val title: String,
    val content: String = "",
    val dueDate: LocalDateTime? = null,
)
