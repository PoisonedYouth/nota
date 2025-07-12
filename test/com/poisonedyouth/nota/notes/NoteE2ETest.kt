package com.poisonedyouth.nota.notes

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class NoteE2ETest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val noteRepository: NoteRepository,
        private val noteService: NoteService,
    ) {

        @LocalServerPort
        private var port: Int = 0

        @BeforeEach
        fun setup() {
            noteRepository.deleteAll()
        }

        @Test
        fun `should return 200 OK when accessing notes endpoint`() {
            // When
            val response = restTemplate.getForEntity("http://localhost:$port/notes", String::class.java)

            // Then
            response.statusCode shouldBe HttpStatus.OK
        }

        @Test
        fun `should create and retrieve notes correctly`() {
            // Given
            val now = LocalDateTime.now()
            val note1 = Note(title = "E2E Test Note 1", content = "E2E Test Content 1", createdAt = now, updatedAt = now)
            val note2 = Note(title = "E2E Test Note 2", content = "E2E Test Content 2", createdAt = now, updatedAt = now.plusHours(1))
            noteRepository.saveAll(listOf(note1, note2))

            // When
            val notes = noteService.findAllNotes()

            // Then
            notes.size shouldBe 2

            // Verify the notes have the expected content
            val titles = notes.map { it.title }
            titles shouldContain "E2E Test Note 1"
            titles shouldContain "E2E Test Note 2"

            // Verify the notes are ordered correctly (most recent first)
            notes[0].title shouldBe "E2E Test Note 2"
            notes[1].title shouldBe "E2E Test Note 1"

            // Verify the DTO methods work correctly
            val firstNote = notes[0]
            firstNote.getContentPreview() shouldContain "E2E Test Content 2"
            firstNote.getFormattedDate() shouldContain now.plusHours(1).year.toString()
        }
    }
