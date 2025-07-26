package com.poisonedyouth.nota.user

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
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
class UserRegistrationE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `complete registration flow should work end-to-end`() {
        val username = "e2euser"

        // Step 1: Show registration form
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/register"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("registerDto"))

        // Step 2: Register new user
        val registrationResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register-success"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("user"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("initialPassword"))
            .andReturn()

        // Step 3: Verify user was created in database
        val createdUser = userRepository.findByUsername(username)
        createdUser shouldNotBe null
        createdUser!!.username shouldBe username
        createdUser.password shouldNotBe "" // Should be hashed

        // Step 4: Extract initial password from response
        val initialPassword = registrationResult.modelAndView?.model?.get("initialPassword") as String
        initialPassword shouldHaveLength 12

        // Step 5: Login with generated password (should redirect to change password)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", username)
                .param("password", initialPassword),
        )
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/auth/change-password"))

        // Step 6: Verify authentication service works with hashed password
        val loginDto = LoginDto(username, initialPassword)
        val authenticatedUser = userService.authenticate(loginDto)
        authenticatedUser shouldNotBe null
        authenticatedUser!!.username shouldBe username
    }

    @Test
    fun `registration should fail for duplicate username`() {
        val username = "duplicateuser"

        // Step 1: Register first user
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register-success"))

        // Step 2: Try to register same username again
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"))
            .andExpect(MockMvcResultMatchers.model().attribute("error", "Username '$username' already exists"))
    }

    @Test
    fun `login should fail with wrong password after registration`() {
        val username = "wrongpassuser"

        // Step 1: Register user
        val registrationResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/register")
                .param("username", username),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/register-success"))
            .andReturn()

        // Step 2: Try to login with wrong password
        mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .param("username", username)
                .param("password", "wrongpassword"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("auth/login"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"))
            .andExpect(MockMvcResultMatchers.model().attribute("error", "Ungültiger Benutzername oder Passwort"))
    }
}
