package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/auth")
class UserController(
    private val userService: UserService,
    private val activityEventPublisher: ActivityEventPublisher,
) {

    @GetMapping("/login")
    fun showLoginForm(model: Model): String {
        model.addAttribute("loginDto", LoginDto("", ""))
        return "auth/login"
    }

    @GetMapping("/register")
    fun showRegisterForm(model: Model): String {
        model.addAttribute("registerDto", RegisterDto(""))
        return "auth/register"
    }

    @PostMapping("/register")
    fun register(
        @ModelAttribute registerDto: RegisterDto,
        model: Model,
    ): String {
        return try {
            val registrationResult = userService.registerUser(registerDto)
            model.addAttribute("user", registrationResult.user)
            model.addAttribute("initialPassword", registrationResult.initialPassword)
            "auth/register-success"
        } catch (e: IllegalArgumentException) {
            model.addAttribute("error", e.message)
            model.addAttribute("registerDto", registerDto)
            "auth/register"
        }
    }

    @PostMapping("/login")
    fun login(
        @ModelAttribute loginDto: LoginDto,
        session: HttpSession,
        model: Model,
    ): String {
        val user = userService.authenticate(loginDto)
        return if (user != null) {
            session.setAttribute("currentUser", user)

            // Publish login event
            activityEventPublisher.publishLoginEvent(user.id)

            if (user.mustChangePassword) {
                "redirect:/auth/change-password"
            } else {
                "redirect:/notes"
            }
        } else {
            model.addAttribute("error", "Ungültiger Benutzername oder Passwort")
            model.addAttribute("loginDto", loginDto)
            "auth/login"
        }
    }

    @GetMapping("/change-password")
    fun showChangePasswordForm(session: HttpSession, model: Model): String {
        val currentUser = session.getAttribute("currentUser") as? UserDto
        return if (currentUser != null) {
            model.addAttribute("changePasswordDto", ChangePasswordDto("", "", ""))
            "auth/change-password"
        } else {
            "redirect:/auth/login"
        }
    }

    @PostMapping("/change-password")
    fun changePassword(
        @ModelAttribute changePasswordDto: ChangePasswordDto,
        session: HttpSession,
        model: Model,
    ): String {
        val currentUser = session.getAttribute("currentUser") as? UserDto
        return if (currentUser != null) {
            try {
                val updatedUser = userService.changePassword(currentUser.username, changePasswordDto)
                session.setAttribute("currentUser", updatedUser)
                "redirect:/notes"
            } catch (e: IllegalArgumentException) {
                model.addAttribute("error", e.message)
                model.addAttribute("changePasswordDto", changePasswordDto)
                "auth/change-password"
            }
        } else {
            "redirect:/auth/login"
        }
    }

    @PostMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/auth/login"
    }
}
