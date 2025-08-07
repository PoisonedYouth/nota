package com.poisonedyouth.nota

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class RootController {
    @GetMapping("/")
    fun root(session: HttpSession): String {
        val currentUser = session.getAttribute("currentUser") as? UserDto
        return if (currentUser != null) {
            "redirect:/notes"
        } else {
            "redirect:/auth/login"
        }
    }
}
