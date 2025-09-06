package com.poisonedyouth.nota.user

import com.poisonedyouth.nota.activitylog.events.ActivityEventPublisher
import com.poisonedyouth.nota.security.SecurityUtils
import jakarta.servlet.http.HttpServletRequest
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
        @jakarta.validation.Valid @ModelAttribute registerDto: RegisterDto,
        bindingResult: org.springframework.validation.BindingResult,
        model: Model,
    ): String =
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDto", registerDto)
            "auth/register"
        } else {
            try {
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
        @jakarta.validation.Valid @ModelAttribute loginDto: LoginDto,
        bindingResult: org.springframework.validation.BindingResult,
        request: HttpServletRequest,
        model: Model,
    ): String {
        val authResult = userService.authenticate(loginDto)
        return when (authResult) {
            is AuthenticationResult.Success -> {
                val user = authResult.user

                // Regenerate session id to prevent session fixation and store current user
                val session = request.getSession(true)
                request.changeSessionId()
                session.setAttribute("currentUser", user)

                // Publish login event
                activityEventPublisher.publishLoginEvent(user.id)

                if (user.mustChangePassword) {
                    "redirect:/auth/change-password"
                } else {
                    "redirect:/notes"
                }
            }
            is AuthenticationResult.UserDisabled -> {
                model.addAttribute("error", "Your account is temporarily disabled. Please contact the administrator.")
                model.addAttribute("loginDto", loginDto)
                "auth/login"
            }
            is AuthenticationResult.InvalidCredentials -> {
                model.addAttribute("error", "Invalid username or password")
                model.addAttribute("loginDto", loginDto)
                "auth/login"
            }
        }
    }

    @GetMapping("/change-password")
    fun showChangePasswordForm(
        session: HttpSession,
        model: Model,
    ): String {
        val currentUser = SecurityUtils.currentUser(session)
        return if (currentUser != null) {
            model.addAttribute("changePasswordDto", ChangePasswordDto("", "", ""))
            "auth/change-password"
        } else {
            "redirect:/auth/login"
        }
    }

    @PostMapping("/change-password")
    fun changePassword(
        @jakarta.validation.Valid @ModelAttribute changePasswordDto: ChangePasswordDto,
        bindingResult: org.springframework.validation.BindingResult,
        request: HttpServletRequest,
        model: Model,
    ): String {
        val currentUser = SecurityUtils.currentUser(request.session)
        return if (currentUser != null) {
            try {
                val updatedUser = userService.changePassword(currentUser.username, changePasswordDto)

                // Regenerate session id after credential change and update current user
                val session = request.getSession(true)
                request.changeSessionId()
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
