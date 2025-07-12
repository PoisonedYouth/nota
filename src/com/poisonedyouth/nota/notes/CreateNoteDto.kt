package com.poisonedyouth.nota.notes

data class CreateNoteDto(
    val title: String,
    val content: String = "",
)
