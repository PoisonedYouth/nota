package com.poisonedyouth.nota.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    @Test
    fun `registerUser should create new user with hashed password and return initial password`() {
        // Given
        val registerDto = RegisterDto("newuser")
        every { userRepository.findByUsername("newuser") } returns null
        every { userRepository.save(any()) } answers {
            val user = firstArg<User>()
            User(
                id = 1L,
                username = user.username,
                password = user.password,
                mustChangePassword = user.mustChangePassword,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
        }

        // When
        val result = userService.registerUser(registerDto)

        // Then
        result.user.username shouldBe "newuser"
        result.user.mustChangePassword shouldBe true
        result.initialPassword shouldHaveLength 12
        result.initialPassword shouldNotBe ""

        verify { userRepository.findByUsername("newuser") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `registerUser should throw exception when username already exists`() {
        // Given
        val registerDto = RegisterDto("existinguser")
        val existingUser = User(
            id = 1L,
            username = "existinguser",
            password = "hashedpassword",
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        every { userRepository.findByUsername("existinguser") } returns existingUser

        // When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.registerUser(registerDto)
        }
        exception.message shouldBe "Username 'existinguser' already exists"

        verify { userRepository.findByUsername("existinguser") }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `authenticate should work with hashed passwords`() {
        // Given
        val plainPassword = "password"
        val loginDto = LoginDto("testuser", plainPassword)

        // Generate BCrypt hash using the same encoder as production
        val passwordEncoder = BCryptPasswordEncoder()
        val hashedPassword = passwordEncoder.encode(plainPassword)

        val user = User(
            id = 1L,
            username = "testuser",
            password = hashedPassword,
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )
        every { userRepository.findByUsername("testuser") } returns user

        // When
        val result = userService.authenticate(loginDto)

        // Then
        result shouldBe AuthenticationResult.Success(UserDto.fromEntity(user))

        verify { userRepository.findByUsername("testuser") }
    }

    @Test
    fun `authenticate should return InvalidCredentials for wrong password`() {
        // Given
        val correctPassword = "password"
        val loginDto = LoginDto("testuser", "wrongpassword")

        // Generate BCrypt hash for correct password
        val passwordEncoder = BCryptPasswordEncoder()
        val hashedPassword = passwordEncoder.encode(correctPassword)

        val user = User(
            id = 1L,
            username = "testuser",
            password = hashedPassword,
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )
        every { userRepository.findByUsername("testuser") } returns user

        // When
        val result = userService.authenticate(loginDto)

        // Then
        result shouldBe AuthenticationResult.InvalidCredentials

        verify { userRepository.findByUsername("testuser") }
    }

    @Test
    fun `authenticate should return InvalidCredentials for non-existent user`() {
        // Given
        val loginDto = LoginDto("nonexistent", "password")
        every { userRepository.findByUsername("nonexistent") } returns null

        // When
        val result = userService.authenticate(loginDto)

        // Then
        result shouldBe AuthenticationResult.InvalidCredentials

        verify { userRepository.findByUsername("nonexistent") }
    }

    @Test
    fun `authenticate should return UserDisabled for disabled user`() {
        // Given
        val plainPassword = "password"
        val loginDto = LoginDto("disableduser", plainPassword)

        // Generate BCrypt hash using the same encoder as production
        val passwordEncoder = BCryptPasswordEncoder()
        val hashedPassword = passwordEncoder.encode(plainPassword)

        val disabledUser = User(
            id = 1L,
            username = "disableduser",
            password = hashedPassword,
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = false,
        )
        every { userRepository.findByUsername("disableduser") } returns disabledUser

        // When
        val result = userService.authenticate(loginDto)

        // Then
        result shouldBe AuthenticationResult.UserDisabled

        verify { userRepository.findByUsername("disableduser") }
    }

    @Test
    fun `createUser should hash password before saving`() {
        // Given
        val username = "testuser"
        val password = "plainpassword"
        val savedUser = User(
            id = 1L,
            username = username,
            password = "hashedpassword",
            mustChangePassword = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )
        every { userRepository.save(any()) } returns savedUser

        // When
        val result = userService.createUser(username, password)

        // Then
        result.username shouldBe username
        result.id shouldBe 1L

        verify {
            userRepository.save(
                match { user ->
                    user.username == username && user.password != password
                },
            )
        }
    }

    @Test
    fun `changePassword should successfully change password and clear mustChangePassword flag`() {
        // Given
        val username = "testuser"
        val currentPassword = "oldpassword"
        val newPassword = "newpassword"
        val changePasswordDto = ChangePasswordDto(currentPassword, newPassword, newPassword)

        val passwordEncoder = BCryptPasswordEncoder()
        val hashedCurrentPassword = passwordEncoder.encode(currentPassword)

        val existingUser = User(
            id = 1L,
            username = username,
            password = hashedCurrentPassword,
            mustChangePassword = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )

        every { userRepository.findByUsername(username) } returns existingUser
        every { userRepository.save(any()) } answers {
            val user = firstArg<User>()
            User(
                id = user.id,
                username = user.username,
                password = user.password,
                mustChangePassword = user.mustChangePassword,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                enabled = user.enabled,
            )
        }

        // When
        val result = userService.changePassword(username, changePasswordDto)

        // Then
        result.username shouldBe username
        result.mustChangePassword shouldBe false

        verify { userRepository.findByUsername(username) }
        verify {
            userRepository.save(
                match { user ->
                    user.username == username &&
                        user.password != hashedCurrentPassword &&
                        user.mustChangePassword == false
                },
            )
        }
    }

    @Test
    fun `changePassword should throw exception when user not found`() {
        // Given
        val username = "nonexistent"
        val changePasswordDto = ChangePasswordDto("old", "new", "new")
        every { userRepository.findByUsername(username) } returns null

        // When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.changePassword(username, changePasswordDto)
        }
        exception.message shouldBe "User not found"

        verify { userRepository.findByUsername(username) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword should throw exception when current password is incorrect`() {
        // Given
        val username = "testuser"
        val correctPassword = "correctpassword"
        val wrongPassword = "wrongpassword"
        val changePasswordDto = ChangePasswordDto(wrongPassword, "newpassword", "newpassword")

        val passwordEncoder = BCryptPasswordEncoder()
        val hashedCorrectPassword = passwordEncoder.encode(correctPassword)

        val existingUser = User(
            id = 1L,
            username = username,
            password = hashedCorrectPassword,
            mustChangePassword = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            enabled = true,
        )

        every { userRepository.findByUsername(username) } returns existingUser

        // When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.changePassword(username, changePasswordDto)
        }
        exception.message shouldBe "Current password is incorrect"

        verify { userRepository.findByUsername(username) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword should throw exception when new password confirmation does not match`() {
        // Given
        val username = "testuser"
        val currentPassword = "currentpassword"
        val changePasswordDto = ChangePasswordDto(currentPassword, "newpassword", "differentpassword")

        val passwordEncoder = BCryptPasswordEncoder()
        val hashedCurrentPassword = passwordEncoder.encode(currentPassword)

        val existingUser = User(
            id = 1L,
            username = username,
            password = hashedCurrentPassword,
            mustChangePassword = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { userRepository.findByUsername(username) } returns existingUser

        // When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.changePassword(username, changePasswordDto)
        }
        exception.message shouldBe "New password and confirmation do not match"

        verify { userRepository.findByUsername(username) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword should throw exception when new password is same as current password`() {
        // Given
        val username = "testuser"
        val currentPassword = "samepassword"
        val changePasswordDto = ChangePasswordDto(currentPassword, currentPassword, currentPassword)

        val passwordEncoder = BCryptPasswordEncoder()
        val hashedCurrentPassword = passwordEncoder.encode(currentPassword)

        val existingUser = User(
            id = 1L,
            username = username,
            password = hashedCurrentPassword,
            mustChangePassword = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { userRepository.findByUsername(username) } returns existingUser

        // When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.changePassword(username, changePasswordDto)
        }
        exception.message shouldBe "New password must be different from current password"

        verify { userRepository.findByUsername(username) }
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
