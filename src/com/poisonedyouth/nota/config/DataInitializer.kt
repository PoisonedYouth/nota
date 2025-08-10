package com.poisonedyouth.nota.config

import com.poisonedyouth.nota.notes.CreateNoteDto
import com.poisonedyouth.nota.notes.NoteService
import com.poisonedyouth.nota.user.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile(value = ["test", "default"])
class DataInitializer(
    private val userService: UserService,
    private val noteService: NoteService,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        // Create a test user if it doesn't exist (only in test profile)
        val testUser = if (userService.findByUsername("testuser") == null) {
            userService.createUser("testuser", "TestPassword123!")
        } else {
            userService.findByUsername("testuser")!!
        }

        // Create sample notes for testing
        try {
            noteService.createNote(
                CreateNoteDto(
                    title = "Sample Note",
                    content = "This is a sample note for testing purposes.",
                ),
                testUser.id,
            )

            noteService.createNote(
                CreateNoteDto(
                    title = "Another Test Note",
                    content = "This is another note to ensure we have multiple notes for UI tests.",
                ),
                testUser.id,
            )
        } catch (e: Exception) {
            // Notes might already exist, ignore
        }
    }
}
