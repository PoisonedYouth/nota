package com.poisonedyouth.nota

import com.poisonedyouth.nota.user.UserDto
import com.poisonedyouth.nota.user.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class RootControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var rootController: RootController

    @BeforeEach
    fun setup() {
        rootController = RootController()
        mockMvc = MockMvcBuilders.standaloneSetup(rootController).build()
    }

    @Test
    fun `should redirect to notes when user is logged in`() {
        // Given
        val session = MockHttpSession()
        val userDto = UserDto(
            id = 1L,
            username = "testuser",
            mustChangePassword = false,
            role = UserRole.USER,
        )
        session.setAttribute("currentUser", userDto)

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))
    }

    @Test
    fun `should redirect to login when no user in session`() {
        // Given
        val session = MockHttpSession()
        // No user in session

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `should redirect to login when session is null`() {
        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `should redirect to login when currentUser attribute is null`() {
        // Given
        val session = MockHttpSession()
        session.setAttribute("currentUser", null)

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `should redirect to login when currentUser is invalid type`() {
        // Given
        val session = MockHttpSession()
        session.setAttribute("currentUser", "invalid_user_object")

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/login"))
    }

    @Test
    fun `should work with admin user`() {
        // Given
        val session = MockHttpSession()
        val adminUser = UserDto(
            id = 2L,
            username = "admin",
            mustChangePassword = false,
            role = UserRole.ADMIN,
        )
        session.setAttribute("currentUser", adminUser)

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/")
                .session(session),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/notes"))
    }
}
