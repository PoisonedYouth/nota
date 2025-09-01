package com.poisonedyouth.nota.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserRestControllerSessionFixationTest {
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
    fun `rest login regenerates session id and sets currentUser`() {
        // Attacker-fixed session exists before login
        val preLoginSession = MockHttpSession()
        val preId = preLoginSession.id

        val loginDto = LoginDto("user", "pw")
        val user = UserDto(1L, "user", false, UserRole.USER)
        every { userService.authenticate(loginDto) } returns AuthenticationResult.Success(user)
        every { activityEventPublisher.publishLoginEvent(1L) } just runs

        val result = mockMvc.perform(
            post("/api/auth/login")
                .session(preLoginSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val postSession = result.request.session
        require(postSession != null)
        assert(preId != postSession.id)
        val currentUser = postSession.getAttribute("currentUser") as UserDto?
        assert(currentUser == user)
    }

    @Test
    fun `rest change-password regenerates session id and updates currentUser`() {
        val existingUser = UserDto(1L, "user", false, UserRole.USER)
        val preSession = MockHttpSession().apply { setAttribute("currentUser", existingUser) }
        val preId = preSession.id

        val changeDto = ChangePasswordDto("old", "newpass", "newpass")
        val updatedUser = existingUser.copy(mustChangePassword = false)
        every { userService.changePassword("user", changeDto) } returns updatedUser

        val result = mockMvc.perform(
            post("/api/auth/change-password")
                .session(preSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeDto)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val postSession = result.request.session
        require(postSession != null)
        assert(preId != postSession.id)
        val currentUser = postSession.getAttribute("currentUser") as UserDto?
        assert(currentUser == updatedUser)
    }
}
