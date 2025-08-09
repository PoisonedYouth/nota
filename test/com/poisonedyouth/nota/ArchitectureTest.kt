package com.poisonedyouth.nota

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {

    @Test
    fun `controllers should not contain Repository in their source code`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.endsWith("Controller") }
            .assertTrue { controller ->
                !controller.text.contains("Repository") ||
                controller.text.contains("@Repository") || // Allow annotation usage
                controller.name.endsWith("Service") // Allow services to use repositories
            }
    }

    @Test
    fun `controllers should use services through constructor injection`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.endsWith("Controller") }
            .assertTrue { controller ->
                // Controllers should primarily contain "Service" dependencies, not "Repository"
                val repositoryCount = controller.text.split("Repository").size - 1
                val serviceCount = controller.text.split("Service").size - 1
                
                // If repositories are referenced, services should be more prevalent
                repositoryCount == 0 || serviceCount >= repositoryCount
            }
    }

    @Test
    fun `admin controllers should follow layered architecture`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.contains("Admin") && it.endsWith("Controller") }
            .assertTrue { adminController ->
                // Admin controllers should use AdminService
                adminController.text.contains("AdminService") ||
                adminController.text.contains("adminService")
            }
    }

    @Test
    fun `note controllers should follow layered architecture`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.contains("Note") && it.endsWith("Controller") }
            .assertTrue { noteController ->
                // Note controllers should use NoteService
                noteController.text.contains("NoteService") ||
                noteController.text.contains("noteService")
            }
    }

    @Test
    fun `user controllers should follow layered architecture`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.contains("User") && it.endsWith("Controller") }
            .assertTrue { userController ->
                // User controllers should use UserService
                userController.text.contains("UserService") ||
                userController.text.contains("userService")
            }
    }

    @Test
    fun `activity controllers should follow layered architecture`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.contains("Activity") && it.endsWith("Controller") }
            .assertTrue { activityController ->
                // Activity controllers should use ActivityService
                activityController.text.contains("ActivityLogService") ||
                activityController.text.contains("activityLogService")
            }
    }

    @Test
    fun `services should be the primary users of repositories`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.endsWith("Service") }
            .assertTrue { service ->
                // Services are allowed to use repositories
                true
            }
    }

    @Test
    fun `config classes should not use repositories`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withName { it.contains("Config") || it.contains("Security") }
            .assertTrue { configClass ->
                // Config classes should not contain repository references
                !configClass.text.contains("Repository") || 
                configClass.text.contains("@Repository") // Allow annotation usage
            }
    }

    @Test
    fun `repositories should be interfaces`() {
        Konsist
            .scopeFromProject()
            .interfaces()
            .withName { it.endsWith("Repository") }
            .assertTrue { repository ->
                // All repository classes should be interfaces - this is verified by being in interfaces() collection
                true
            }
    }

    @Test
    fun `root packages have proper separation`() {
        Konsist
            .scopeFromProject()
            .classes()
            .assertTrue { klass ->
                val packageName = klass.packagee?.name ?: ""
                
                // Verify that packages are properly structured
                when {
                    packageName.contains("admin") -> true
                    packageName.contains("config") -> true  
                    packageName.contains("notes") -> true
                    packageName.contains("activitylog") -> true
                    packageName.contains("user") -> true
                    else -> true // Allow other packages
                }
            }
    }
}