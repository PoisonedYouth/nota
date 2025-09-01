package com.poisonedyouth.nota

import com.poisonedyouth.nota.security.SecurityUtils
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class RootController {
    @GetMapping("/")
    fun root(session: HttpSession): String {
        val currentUser = SecurityUtils.currentUser(session)
        return if (currentUser != null) {
            "redirect:/notes"
        } else {
            "redirect:/auth/login"
        }
    }
}
