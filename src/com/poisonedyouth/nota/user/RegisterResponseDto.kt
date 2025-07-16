package com.poisonedyouth.nota.user

data class RegisterResponseDto(
    val user: UserDto,
    val initialPassword: String,
)
