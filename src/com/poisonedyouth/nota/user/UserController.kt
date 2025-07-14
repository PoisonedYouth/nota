package com.poisonedyouth.nota.user

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
) {

    @GetMapping("/login")
    fun showLoginForm(model: Model): String {
        model.addAttribute("loginDto", LoginDto("", ""))
        return "auth/login"
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
            "redirect:/notes"
        } else {
            model.addAttribute("error", "Invalid username or password")
            model.addAttribute("loginDto", loginDto)
            "auth/login"
        }
    }

    @PostMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/auth/login"
    }
}
