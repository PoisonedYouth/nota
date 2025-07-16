package com.poisonedyouth.nota.user

data class ChangePasswordDto(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)
