package com.poisonedyouth.nota.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
) {

    fun authenticate(loginDto: LoginDto): UserDto? {
        val user = userRepository.findByUsername(loginDto.username)
        return if (user != null && user.password == loginDto.password) {
            UserDto.fromEntity(user)
        } else {
            null
        }
    }

    fun createUser(username: String, password: String): UserDto {
        val user = User(
            username = username,
            password = password,
        )
        val savedUser = userRepository.save(user)
        return UserDto.fromEntity(savedUser)
    }

    fun findByUsername(username: String): UserDto? {
        val user = userRepository.findByUsername(username)
        return user?.let { UserDto.fromEntity(it) }
    }
}
