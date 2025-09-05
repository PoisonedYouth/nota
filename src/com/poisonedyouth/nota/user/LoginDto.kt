package com.poisonedyouth.nota.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginDto(
    @field:NotBlank(message = "Username is required")
    @field:Size(max = 64)
    val username: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128)
    val password: String,
)
