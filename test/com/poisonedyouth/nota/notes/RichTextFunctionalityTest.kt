package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.Optional

class RichTextFunctionalityTest {

    private lateinit var noteService: NoteService
    private lateinit var noteRepository: NoteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var noteShareRepository: NoteShareRepository

    private val testUser = User(
        id = 1L,
        username = "testuser",
        password = "password",
    )

    @BeforeEach
    fun setup() {
        noteRepository = mockk()
        userRepository = mockk()
        noteShareRepository = mockk()
        noteService = NoteService(noteRepository, userRepository, noteShareRepository)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
    }

    @Test
    fun `should allow safe HTML tags in note content`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Rich Text Note",
            content = "<p>This is <strong>bold</strong> and <em>italic</em> text.</p><ul><li>Item 1</li><li>Item 2</li></ul>",
        )
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = createNoteDto.content,
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldContain "<p>"
        result.content shouldContain "<strong>"
        result.content shouldContain "<em>"
        result.content shouldContain "<ul>"
        result.content shouldContain "<li>"
        verify { noteRepository.save(any()) }
    }

    @Test
    fun `should remove dangerous script tags from content`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Malicious Note",
            content = "<p>Safe content</p><script>alert('XSS')</script><p>More safe content</p>",
        )
        val expectedSanitizedContent = "<p>Safe content</p><p>More safe content</p>"
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = expectedSanitizedContent,
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldNotContain "<script>"
        result.content shouldNotContain "alert"
        result.content shouldContain "<p>Safe content</p>"
        result.content shouldContain "<p>More safe content</p>"
    }

    @Test
    fun `should remove iframe and other dangerous tags`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Dangerous Note",
            content = "<p>Safe content</p><iframe src='http://evil.com'></iframe><object data='malware.exe'></object>",
        )
        val expectedSanitizedContent = "<p>Safe content</p>"
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = expectedSanitizedContent,
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldNotContain "<iframe>"
        result.content shouldNotContain "<object>"
        result.content shouldContain "<p>Safe content</p>"
    }

    @Test
    fun `should sanitize anchor tags and allow safe URLs`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Link Note",
            content = "<p>Check out <a href='https://example.com'>this link</a> and <a href='javascript:alert(1)'>this bad link</a></p>",
        )
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = "<p>Check out <a href=\"https://example.com\">this link</a> and <a>this bad link</a></p>",
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldContain "href=\"https://example.com\""
        result.content shouldNotContain "javascript:"
    }

    @Test
    fun `should remove event handlers from HTML`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Event Handler Note",
            content = "<p onclick='alert(1)'>Click me</p><div onmouseover='steal()'>Hover me</div>",
        )
        val expectedSanitizedContent = "<p>Click me</p>"
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = expectedSanitizedContent,
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldNotContain "onclick"
        result.content shouldNotContain "onmouseover"
        result.content shouldNotContain "alert"
        result.content shouldNotContain "steal"
    }

    @Test
    fun `should sanitize content when updating notes`() {
        // Given
        val existingNote = Note(
            id = 1L,
            title = "Existing Note",
            content = "<p>Old content</p>",
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updateNoteDto = UpdateNoteDto(
            id = 1L,
            title = "Updated Note",
            content = "<p>New content</p><script>alert('XSS')</script>",
        )
        val updatedNote = Note(
            id = 1L,
            title = updateNoteDto.title,
            content = "<p>New content</p>",
            user = testUser,
            createdAt = existingNote.createdAt,
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.findByIdAndUser(1L, testUser) } returns existingNote
        every { noteRepository.save(any()) } returns updatedNote

        // When
        val result = noteService.updateNote(updateNoteDto, 1L)

        // Then
        result?.content shouldBe "<p>New content</p>"
        result?.content shouldNotContain "<script>"
        result?.content shouldNotContain "alert"
    }

    @Test
    fun `should preserve Quill editor formatting`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Formatted Note",
            content = """
                <h1>Main Title</h1>
                <h2>Subtitle</h2>
                <p>This is a paragraph with <strong>bold</strong>, <em>italic</em>, and <u>underlined</u> text.</p>
                <blockquote>This is a quote</blockquote>
                <ul>
                    <li>First item</li>
                    <li>Second item</li>
                </ul>
                <ol>
                    <li>Numbered item 1</li>
                    <li>Numbered item 2</li>
                </ol>
                <pre><code>Code block</code></pre>
            """.trimIndent(),
        )
        val savedNote = Note(
            id = 1L,
            title = createNoteDto.title,
            content = createNoteDto.content,
            user = testUser,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { noteRepository.save(any()) } returns savedNote

        // When
        val result = noteService.createNote(createNoteDto, 1L)

        // Then
        result.content shouldContain "<h1>"
        result.content shouldContain "<h2>"
        result.content shouldContain "<strong>"
        result.content shouldContain "<em>"
        result.content shouldContain "<u>"
        result.content shouldContain "<blockquote>"
        result.content shouldContain "<ul>"
        result.content shouldContain "<ol>"
        result.content shouldContain "<li>"
        result.content shouldContain "<pre>"
        result.content shouldContain "<code>"
    }

    @Test
    fun `should throw exception for empty content`() {
        // Given
        val createNoteDto = CreateNoteDto(
            title = "Empty Note",
            content = "",
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            noteService.createNote(createNoteDto, 1L)
        }
    }
}
