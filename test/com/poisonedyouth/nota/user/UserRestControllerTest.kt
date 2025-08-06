package com.poisonedyouth.nota.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserRestControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var userService: UserService
    private lateinit var activityEventPublisher: ActivityEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        userService = mockk()
        activityEventPublisher = mockk()
        objectMapper = ObjectMapper()
        val controller = UserRestController(userService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should register user successfully`() {
        // Given
        val registerDto = RegisterDto("testuser")
        val userDto = UserDto(
            id = 1L,
            username = "testuser",
            mustChangePassword = true,
            role = UserRole.USER,
        )
        val registerResponseDto = RegisterResponseDto(userDto, "generated123")

        every { userService.registerUser(registerDto) } returns registerResponseDto

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.username").value("testuser"))
            .andExpect(jsonPath("$.user.mustChangePassword").value(true))
            .andExpect(jsonPath("$.initialPassword").value("generated123"))
    }

    @Test
    fun `should return error when username already exists`() {
        // Given
        val registerDto = RegisterDto("existinguser")
        every { userService.registerUser(registerDto) } throws IllegalArgumentException("Username 'existinguser' already exists")

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Username 'existinguser' already exists"))
    }

    @Test
    fun `should login user successfully`() {
        // Given
        val loginDto = LoginDto("testuser", "password")
        val userDto = UserDto(
            id = 1L,
            username = "testuser",
            mustChangePassword = false,
            role = UserRole.USER,
        )
        val authResult = AuthenticationResult.Success(userDto)

        every { userService.authenticate(loginDto) } returns authResult
        every { activityEventPublisher.publishLoginEvent(1L) } just runs

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.username").value("testuser"))
            .andExpect(jsonPath("$.user.mustChangePassword").value(false))
            .andExpect(jsonPath("$.mustChangePassword").value(false))

        verify { activityEventPublisher.publishLoginEvent(1L) }
    }

    @Test
    fun `should return unauthorized for invalid credentials`() {
        // Given
        val loginDto = LoginDto("testuser", "wrongpassword")
        every { userService.authenticate(loginDto) } returns AuthenticationResult.InvalidCredentials

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }

    @Test
    fun `should return forbidden for disabled user`() {
        // Given
        val loginDto = LoginDto("disableduser", "password")
        every { userService.authenticate(loginDto) } returns AuthenticationResult.UserDisabled

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("Account is temporarily disabled. Please contact the administrator."))
    }
}
