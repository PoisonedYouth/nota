package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.activitylog.ActivityLogService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var userService: UserService
    private lateinit var activityLogService: ActivityLogService

    @BeforeEach
    fun setup() {
        userService = mockk()
        activityLogService = mockk()
        val controller = UserController(userService, activityLogService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `showRegisterForm should return register view`() {
        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/register"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("registerDto"))
    }

    @Test
    fun `register should create user and return success view`() {
        // Given
        val username = "newuser"
        val userDto = UserDto(id = 1L, username = username, mustChangePassword = false)
        val initialPassword = "ABC123def456"
        val registerResponse = RegisterResponseDto(userDto, initialPassword)

        every { userService.registerUser(any()) } returns registerResponse

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register-success"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("user"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("initialPassword"))
            .andExpect(MockMvcResultMatchers.model().attribute("user", userDto))
            .andExpect(MockMvcResultMatchers.model().attribute("initialPassword", initialPassword))

        verify { userService.registerUser(match { it.username == username }) }
    }

    @Test
    fun `register should return error when username already exists`() {
        // Given
        val username = "existinguser"
        every { userService.registerUser(any()) } throws IllegalArgumentException("Username 'existinguser' already exists")

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("registerDto"))
            .andExpect(MockMvcResultMatchers.model().attribute("error", "Username 'existinguser' already exists"))

        verify { userService.registerUser(match { it.username == username }) }
    }

    @Test
    fun `showLoginForm should return login view`() {
        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/login"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/login"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("loginDto"))
    }

    @Test
    fun `login should authenticate user and redirect to notes when password change not required`() {
        // Given
        val username = "testuser"
        val password = "password"
        val userDto = UserDto(id = 1L, username = username, mustChangePassword = false)

        every { userService.authenticate(any()) } returns userDto
        every { activityLogService.logActivity(any(), any(), any(), any(), any()) } just Runs

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", username)
                .param("password", password),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))

        verify { userService.authenticate(match { it.username == username && it.password == password }) }
    }

    @Test
    fun `login should authenticate user and redirect to change password when password change required`() {
        // Given
        val username = "testuser"
        val password = "password"
        val userDto = UserDto(id = 1L, username = username, mustChangePassword = true)

        every { userService.authenticate(any()) } returns userDto
        every { activityLogService.logActivity(any(), any(), any(), any(), any()) } just Runs

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", username)
                .param("password", password),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/change-password"))

        verify { userService.authenticate(match { it.username == username && it.password == password }) }
    }

    @Test
    fun `login should return error for invalid credentials`() {
        // Given
        val username = "testuser"
        val password = "wrongpassword"

        every { userService.authenticate(any()) } returns null

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", username)
                .param("password", password),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/login"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("loginDto"))
            .andExpect(MockMvcResultMatchers.model().attribute("error", "Ungültiger Benutzername oder Passwort"))

        verify { userService.authenticate(match { it.username == username && it.password == password }) }
    }
}
