package com.poisonedyouth.nota.notes

import java.time.LocalDateTime

data class UpdateNoteDto(
    val id: Long,
    val title: String,
    val content: String = "",
    val dueDate: LocalDateTime? = null,
)
