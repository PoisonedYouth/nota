package com.poisonedyouth.nota.config

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersIT {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `responses include security headers`() {
        val result = mockMvc.perform(get("/auth/login")).andReturn()
        val response = result.response

        response.getHeader("X-Content-Type-Options").shouldContain("nosniff")
        response.getHeader("X-Frame-Options").shouldContain("DENY")
        response.getHeader("Content-Security-Policy").shouldContain("default-src 'self'")
        response.getHeader("Content-Security-Policy").shouldContain("object-src 'none'")
        response.getHeader("Content-Security-Policy").shouldContain("frame-ancestors 'none'")
    }
}
