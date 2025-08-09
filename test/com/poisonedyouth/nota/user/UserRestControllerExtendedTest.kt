package com.poisonedyouth.nota.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserRestControllerExtendedTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var userService: UserService
    private lateinit var activityEventPublisher: ActivityEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        userService = mockk()
        activityEventPublisher = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        val controller = UserRestController(userService, activityEventPublisher)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun sessionWithUser(): MockHttpSession = MockHttpSession().apply {
        setAttribute("currentUser", UserDto(1L, "user", false, UserRole.USER))
    }

    @Test
    fun `should return current user`() {
        mockMvc
            .perform(get("/api/auth/me").session(sessionWithUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.username").value("user"))
    }

    @Test
    fun `should change password`() {
        val dto = ChangePasswordDto("old", "newpass", "newpass")
        every { userService.changePassword("user", dto) } returns UserDto(1L, "user", false, UserRole.USER)

        mockMvc
            .perform(
                post("/api/auth/change-password")
                    .session(sessionWithUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.username").value("user"))
    }

    @Test
    fun `should logout`() {
        mockMvc
            .perform(post("/api/auth/logout").session(sessionWithUser()))
            .andExpect(status().isNoContent)
    }
}
