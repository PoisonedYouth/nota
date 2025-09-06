package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.common.EntityMapperUtils

data class UserDto(
    val id: Long,
    val username: String,
    val mustChangePassword: Boolean,
    val role: UserRole,
    val version: Long = 0,
) {
    companion object {
        fun fromEntity(user: User): UserDto =
            UserDto(
                id = EntityMapperUtils.validateId(user.id, "User"),
                username = user.username,
                mustChangePassword = user.mustChangePassword,
                role = user.role,
                version = user.version,
            )

        fun fromEntityList(users: List<User>): List<UserDto> =
            EntityMapperUtils.mapList(users) { fromEntity(it) }
    }

    fun isAdmin(): Boolean = role == UserRole.ADMIN

    fun shouldChangePassword(): Boolean = mustChangePassword
}
