package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class UserRestController(
    private val userService: UserService,
    private val activityEventPublisher: ActivityEventPublisher,
) {
    @PostMapping("/register")
    fun register(
        @RequestBody registerDto: RegisterDto,
    ): ResponseEntity<*> =
        try {
            val registrationResult = userService.registerUser(registerDto)
            ResponseEntity.ok(registrationResult)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @PostMapping("/login")
    fun login(
        @RequestBody loginDto: LoginDto,
    ): ResponseEntity<*> {
        val authResult = userService.authenticate(loginDto)
        return when (authResult) {
            is AuthenticationResult.Success -> {
                val user = authResult.user

                // Publish login event
                activityEventPublisher.publishLoginEvent(user.id)

                ResponseEntity.ok(
                    mapOf(
                        "user" to user,
                        "mustChangePassword" to user.mustChangePassword,
                    ),
                )
            }
            is AuthenticationResult.UserDisabled -> {
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Account is temporarily disabled. Please contact the administrator."))
            }
            is AuthenticationResult.InvalidCredentials -> {
                ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Invalid username or password"))
            }
        }
    }
}
