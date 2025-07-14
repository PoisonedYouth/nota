package com.poisonedyouth.nota.user

data class UserDto(
    val id: Long,
    val username: String,
) {
    companion object {
        fun fromEntity(user: User): UserDto {
            return UserDto(
                id = user.id!!,
                username = user.username,
            )
        }
    }
}
