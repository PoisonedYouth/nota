package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteSanitizationAndAccessTest
    @Autowired
    constructor(
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository,
        private val noteService: NoteService,
    ) {
        private lateinit var alice: User
        private lateinit var bob: User

        @BeforeEach
        fun setup() {
            noteRepository.deleteAll()
            userRepository.deleteAll()
            alice = userRepository.save(User(username = "alice_${System.currentTimeMillis()}", password = "pw"))
            bob = userRepository.save(User(username = "bob_${System.currentTimeMillis()}", password = "pw"))
        }

        @Test
        fun `createNote should sanitize dangerous HTML content`() {
            // Given: payload with scripts, JS href and event handler
            val dirty = """
            <div>
              <h1 onclick=\"alert('x')\">Title</h1>
              <script>alert('x')</script>
              <a href=\"javascript:alert('x')\">bad link</a>
              <a href=\"https://example.com\">good</a>
              <p>Text</p>
            </div>
            """.trimIndent()

            // When
            val dto = noteService.createNote(CreateNoteDto(title = "t", content = dirty), alice.id!!)

            // Then: script removed, event handlers removed, javascript: removed
            dto.content.shouldNotContain("<script>")
            dto.content.shouldNotContain("onclick=")
            dto.content.shouldNotContain("javascript:")
            // Note: OWASP sanitizer removes links by default for security, keeping only text content
            dto.content.shouldContain("good") // Link text is preserved
            dto.content.shouldContain("Title") // Title text is preserved
            dto.content.shouldContain("Text") // Paragraph text is preserved
        }

        @Test
        fun `updateNote should sanitize dangerous HTML content`() {
            // Given
            val created = noteService.createNote(CreateNoteDto(title = "t", content = "<p>ok</p>"), alice.id!!)
            val tricky = "<img src=\"x\" onerror=\"alert(1)\">Safe text<script>1</script>"

            // When
            val updated = noteService.updateNote(UpdateNoteDto(id = created.id, title = "t2", content = tricky), alice.id!!)

            // Then
            requireNotNull(updated)
            updated.content.shouldNotContain("onerror=")
            updated.content.shouldNotContain("<script>")
            updated.content.shouldNotContain("<img")
            // Note: OWASP sanitizer removes dangerous tags but preserves safe text content
            updated.content.shouldContain("Safe text")
        }

        @Test
        fun `findAllNotes excludes archived and findAllArchivedNotes returns only archived`() {
            // Given
            val now = LocalDateTime.now()
            val n1 = noteRepository.save(Note(title = "a", content = "c", user = alice, createdAt = now, updatedAt = now))
            val n2 = noteRepository.save(
                Note(title = "b", content = "c", user = alice, createdAt = now, updatedAt = now, archived = true, archivedAt = now),
            )

            // When
            val active = noteService.findAllNotes(alice.id!!)
            val archived = noteService.findAllArchivedNotes(alice.id!!)

            // Then
            active.shouldHaveSize(1)
            active[0].id shouldBe n1.id
            archived.shouldHaveSize(1)
            archived[0].id shouldBe n2.id
        }

        @Test
        fun `per-user access control - user cannot access other's note`() {
            // Given
            val created = noteService.createNote(CreateNoteDto(title = "t", content = "ok"), alice.id!!)

            // When
            val asBob = noteService.findNoteById(created.id, bob.id!!)
            val accessibleAsBob = noteService.findAccessibleNoteById(created.id, bob.id!!)

            // Then
            asBob shouldBe null
            accessibleAsBob shouldBe null
        }
    }
