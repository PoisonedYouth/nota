package com.poisonedyouth.nota.config

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CookieSecurityIT {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: com.poisonedyouth.nota.user.UserService

    @Test
    fun `csrf cookie should be Secure and NOT HttpOnly`() {
        // First GET should trigger CSRF token creation and cookie via CookieCsrfTokenRepository
        val result = mockMvc.perform(get("/auth/login")).andReturn()
        val setCookies = result.response.getHeaders("Set-Cookie")
        setCookies.shouldNotBeNull()
        setCookies.isEmpty().shouldBeFalse()

        val csrfCookie = setCookies.firstOrNull { it.startsWith("XSRF-TOKEN=") }
        csrfCookie.shouldNotBeNull()
        csrfCookie!!
            .apply {
                shouldContain("Secure")
                // Must be readable by JS (HttpOnly must be absent)
                shouldNotContain("HttpOnly")
            }
    }
}
