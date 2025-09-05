package com.poisonedyouth.nota.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterDto(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 64)
    val username: String,
)
