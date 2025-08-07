package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.user.UserDto
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService,
) {
    /**
     * Check if current user is admin and redirect to login if not authenticated
     */
    private fun checkAdminAccess(session: HttpSession): UserDto? {
        val currentUser = session.getAttribute("currentUser") as? UserDto
        return if (currentUser != null && adminService.isAdmin(currentUser.username)) {
            currentUser
        } else {
            null
        }
    }

    /**
     * Show admin overview with user statistics
     */
    @GetMapping("/overview")
    fun showAdminOverview(
        session: HttpSession,
        model: Model,
    ): String {
        val currentUser = checkAdminAccess(session)

        return if (currentUser != null) {
            val userStatistics = adminService.getAllUserStatistics()
            val systemStatistics = adminService.getSystemStatistics()

            model.addAttribute("currentUser", currentUser)
            model.addAttribute("userStatistics", userStatistics)
            model.addAttribute("systemStatistics", systemStatistics)

            "admin/overview"
        } else {
            "redirect:/auth/login"
        }
    }

    /**
     * Redirect root admin path to overview
     */
    @GetMapping("")
    fun redirectToOverview(): String = "redirect:/admin/overview"

    /**
     * Disable a user
     */
    @PostMapping("/users/{userId}/disable")
    fun disableUser(
        @PathVariable userId: Long,
        session: HttpSession,
        redirectAttributes: RedirectAttributes,
    ): String {
        val currentUser = checkAdminAccess(session)

        return if (currentUser != null) {
            val success = adminService.disableUser(userId)
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "User has been disabled successfully")
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to disable user. Admin users cannot be disabled.")
            }
            "redirect:/admin/overview"
        } else {
            "redirect:/auth/login"
        }
    }

    /**
     * Enable a user
     */
    @PostMapping("/users/{userId}/enable")
    fun enableUser(
        @PathVariable userId: Long,
        session: HttpSession,
        redirectAttributes: RedirectAttributes,
    ): String {
        val currentUser = checkAdminAccess(session)

        return if (currentUser != null) {
            val success = adminService.enableUser(userId)
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "User has been enabled successfully")
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to enable user")
            }
            "redirect:/admin/overview"
        } else {
            "redirect:/auth/login"
        }
    }
}
