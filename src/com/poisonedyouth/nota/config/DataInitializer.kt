package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.user.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val userService: UserService,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Create a test user if it doesn't exist
        if (userService.findByUsername("testuser") == null) {
            userService.createUser("testuser", "password")
            println("Created test user: testuser / password")
        }
    }
}
