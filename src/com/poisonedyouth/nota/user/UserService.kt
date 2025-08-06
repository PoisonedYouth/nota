package com.poisonedyouth.nota.user

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    private fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }

    private fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return passwordEncoder.matches(password, hashedPassword)
    }

    private fun validatePasswordComplexity(password: String) {
        require(password.length >= 12) { "Password must be at least 12 characters long" }
        require(password.any { it.isUpperCase() }) { "Password must contain at least one uppercase letter" }
        require(password.any { it.isLowerCase() }) { "Password must contain at least one lowercase letter" }
        require(password.any { it.isDigit() }) { "Password must contain at least one digit" }
        require(password.any { !it.isLetterOrDigit() }) { "Password must contain at least one special character" }
    }

    private fun generateInitialPassword(): String {
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789$specialChars"
        val random = SecureRandom()
        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun authenticate(loginDto: LoginDto): AuthenticationResult {
        val user = userRepository.findByUsername(loginDto.username)

        return when {
            user == null -> AuthenticationResult.InvalidCredentials
            !verifyPassword(loginDto.password, user.password) -> AuthenticationResult.InvalidCredentials
            !user.enabled -> AuthenticationResult.UserDisabled
            else -> AuthenticationResult.Success(UserDto.fromEntity(user))
        }
    }

    fun createUser(username: String, password: String): UserDto {
        validatePasswordComplexity(password)
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
        if (!verifyPassword(changePasswordDto.currentPassword, user.password)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Validate new password confirmation
        if (changePasswordDto.newPassword != changePasswordDto.confirmPassword) {
            throw IllegalArgumentException("New password and confirmation do not match")
        }

        // Validate new password complexity
        validatePasswordComplexity(changePasswordDto.newPassword)

        // Validate new password is different from current
        if (verifyPassword(changePasswordDto.newPassword, user.password)) {
            throw IllegalArgumentException("New password must be different from current password")
        }

        val hashedNewPassword = hashPassword(changePasswordDto.newPassword)

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
