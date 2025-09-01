package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserControllerSessionFixationTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var userService: UserService
    private lateinit var activityEventPublisher: ActivityEventPublisher

    @BeforeEach
    fun setup() {
        userService = mockk()
        activityEventPublisher = mockk()
        val controller = UserController(userService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `login regenerates session id and sets currentUser`() {
        // Given an existing (attacker-fixed) session
        val preLoginSession = MockHttpSession()
        val preId = preLoginSession.id

        val user = UserDto(1L, "user", false, UserRole.USER)
        every { userService.authenticate(LoginDto("user", "pw")) } returns AuthenticationResult.Success(user)
        every { activityEventPublisher.publishLoginEvent(1L) } just Runs

        // When
        val result = mockMvc.perform(
            post("/auth/login")
                .session(preLoginSession)
                .param("username", "user")
                .param("password", "pw"),
        )
            // Then
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/notes"))
            .andReturn()

        val postSession = result.request.session
        require(postSession != null)
        // Session id must change
        assert(preId != postSession.id)
        // currentUser must be present on the new session
        val currentUser = postSession.getAttribute("currentUser") as UserDto?
        assert(currentUser == user)
    }

    @Test
    fun `change-password regenerates session id and updates currentUser`() {
        // Given a logged-in user session
        val existingUser = UserDto(1L, "user", true, UserRole.USER)
        val preSession = MockHttpSession().apply { setAttribute("currentUser", existingUser) }
        val preId = preSession.id

        val updatedUser = existingUser.copy(mustChangePassword = false)
        every { userService.changePassword("user", ChangePasswordDto("old", "newpass", "newpass")) } returns updatedUser

        // When
        val result = mockMvc.perform(
            post("/auth/change-password")
                .session(preSession)
                .param("currentPassword", "old")
                .param("newPassword", "newpass")
                .param("confirmPassword", "newpass"),
        )
            // Then
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/notes"))
            .andReturn()

        val postSession = result.request.session
        require(postSession != null)
        assert(preId != postSession.id)
        val currentUser = postSession.getAttribute("currentUser") as UserDto?
        assert(currentUser == updatedUser)
    }
}
