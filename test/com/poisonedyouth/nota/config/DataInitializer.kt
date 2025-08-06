package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.user.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class DataInitializer(
    private val userService: UserService,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Create a test user if it doesn't exist (only in test profile)
        if (userService.findByUsername("testuser") == null) {
            userService.createUser("testuser", "TestPassword123!")
            println("Created test user: testuser / TestPassword123!")
        }
    }
}
