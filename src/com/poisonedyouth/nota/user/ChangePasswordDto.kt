package com.poisonedyouth.nota.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordDto(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128)
    val newPassword: String,
    @field:NotBlank(message = "Confirm password is required")
    val confirmPassword: String,
)
