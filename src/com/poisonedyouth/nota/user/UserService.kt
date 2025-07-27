package com.poisonedyouth.nota.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
) {

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateInitialPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..12)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun authenticate(loginDto: LoginDto): AuthenticationResult {
        val user = userRepository.findByUsername(loginDto.username)
        val hashedPassword = hashPassword(loginDto.password)

        return when {
            user == null -> AuthenticationResult.InvalidCredentials
            user.password != hashedPassword -> AuthenticationResult.InvalidCredentials
            !user.enabled -> AuthenticationResult.UserDisabled
            else -> AuthenticationResult.Success(UserDto.fromEntity(user))
        }
    }

    fun createUser(username: String, password: String): UserDto {
        val hashedPassword = hashPassword(password)
        val user = User(
            username = username,
            password = hashedPassword,
        )
        val savedUser = userRepository.save(user)
        return UserDto.fromEntity(savedUser)
    }

    fun registerUser(registerDto: RegisterDto): RegisterResponseDto {
        // Check if username already exists
        val existingUser = userRepository.findByUsername(registerDto.username)
        if (existingUser != null) {
            throw IllegalArgumentException("Username '${registerDto.username}' already exists")
        }

        // Generate initial password
        val initialPassword = generateInitialPassword()

        // Create user with hashed password and require password change
        val hashedPassword = hashPassword(initialPassword)
        val user = User(
            username = registerDto.username,
            password = hashedPassword,
            mustChangePassword = true,
        )
        val savedUser = userRepository.save(user)

        return RegisterResponseDto(
            user = UserDto.fromEntity(savedUser),
            initialPassword = initialPassword,
        )
    }

    fun changePassword(username: String, changePasswordDto: ChangePasswordDto): UserDto {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")

        // Verify current password
        val hashedCurrentPassword = hashPassword(changePasswordDto.currentPassword)
        if (user.password != hashedCurrentPassword) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Validate new password confirmation
        if (changePasswordDto.newPassword != changePasswordDto.confirmPassword) {
            throw IllegalArgumentException("New password and confirmation do not match")
        }

        // Validate new password is different from current
        val hashedNewPassword = hashPassword(changePasswordDto.newPassword)
        if (user.password == hashedNewPassword) {
            throw IllegalArgumentException("New password must be different from current password")
        }

        // Update password and clear mustChangePassword flag
        val updatedUser = User(
            id = user.id,
            username = user.username,
            password = hashedNewPassword,
            mustChangePassword = false,
            createdAt = user.createdAt,
            updatedAt = java.time.LocalDateTime.now(),
        )
        val savedUser = userRepository.save(updatedUser)
        return UserDto.fromEntity(savedUser)
    }

    fun findByUsername(username: String): UserDto? {
        val user = userRepository.findByUsername(username)
        return user?.let { UserDto.fromEntity(it) }
    }
}
