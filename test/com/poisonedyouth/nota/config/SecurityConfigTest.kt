package com.poisonedyouth.nota.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should allow access to auth login endpoint`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/auth/login"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should allow access to auth register endpoint`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/auth/register"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should allow POST to auth login`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/auth/login")
                    .param("username", "testuser")
                    .param("password", "TestPassword123!"),
            ).andExpect(MockMvcResultMatchers.status().is3xxRedirection)
    }

    @Test
    fun `should allow POST to auth register`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/auth/register")
                    .param("username", "newuser_${System.currentTimeMillis()}"),
            ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should allow access to static resources`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/css/main.css"))
            .andExpect(MockMvcResultMatchers.status().isOk) // Static resources are served even if file doesn't exist
    }

    @Test
    fun `should allow access to actuator endpoints`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should require authentication for protected endpoints`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notes"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `should require authentication for admin endpoints`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/admin/overview"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `should block access to unknown protected endpoints`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/protected/endpoint"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}
