package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class NoteRepositoryTest
    @Autowired
    constructor(
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository,
        private val noteShareRepository: NoteShareRepository,
    ) {
        private lateinit var testUser1: User
        private lateinit var testUser2: User
        private lateinit var note1: Note
        private lateinit var note2: Note
        private lateinit var archivedNote: Note

        @BeforeEach
        fun setup() {
            noteShareRepository.deleteAll()
            noteRepository.deleteAll()
            userRepository.deleteAll()

            testUser1 =
                userRepository.save(
                    User(
                        username = "testuser1_${System.currentTimeMillis()}",
                        password = "hashedPassword1",
                    ),
                )

            testUser2 =
                userRepository.save(
                    User(
                        username = "testuser2_${System.currentTimeMillis()}",
                        password = "hashedPassword2",
                    ),
                )

            note1 =
                noteRepository.save(
                    Note(
                        title = "Test Note 1",
                        content = "This is the content of test note 1",
                        user = testUser1,
                        createdAt = LocalDateTime.now().minusHours(2),
                        updatedAt = LocalDateTime.now().minusHours(1),
                    ),
                )

            note2 =
                noteRepository.save(
                    Note(
                        title = "Another Note",
                        content = "Different content here",
                        user = testUser1,
                        createdAt = LocalDateTime.now().minusHours(1),
                        updatedAt = LocalDateTime.now(),
                    ),
                )

            archivedNote =
                noteRepository.save(
                    Note(
                        title = "Archived Note",
                        content = "This note is archived",
                        user = testUser1,
                        archived = true,
                        createdAt = LocalDateTime.now().minusHours(3),
                        updatedAt = LocalDateTime.now().minusHours(2),
                    ),
                )
        }

        @Test
        fun `findAllByUserAndArchivedFalseOrderByUpdatedAtDesc should return non-archived notes in desc order`() {
            // When
            val notes = noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser1)

            // Then
            notes shouldHaveSize 2
            notes shouldContain note1
            notes shouldContain note2
            notes shouldNotContain archivedNote
            // Should be ordered by updatedAt desc (note2 is newer)
            notes[0] shouldBe note2
            notes[1] shouldBe note1
        }

        @Test
        fun `findAllByUserAndArchivedTrueOrderByUpdatedAtDesc should return only archived notes`() {
            // When
            val notes = noteRepository.findAllByUserAndArchivedTrueOrderByUpdatedAtDesc(testUser1)

            // Then
            notes shouldHaveSize 1
            notes shouldContain archivedNote
            notes shouldNotContain note1
            notes shouldNotContain note2
        }

        @Test
        fun `findByIdAndUser should return note only if belongs to user`() {
            // When
            val foundNote = noteRepository.findByIdAndUser(note1.id!!, testUser1)
            val notFoundNote = noteRepository.findByIdAndUser(note1.id!!, testUser2)

            // Then
            foundNote shouldBe note1
            notFoundNote shouldBe null
        }

        @Test
        fun `count methods should return correct statistics`() {
            // When
            val totalCount = noteRepository.countByUser(testUser1)
            val archivedCount = noteRepository.countByUserAndArchivedTrue(testUser1)
            val totalArchivedCount = noteRepository.countByArchivedTrue()

            // Then
            totalCount shouldBe 3 // note1, note2, archivedNote
            archivedCount shouldBe 1 // only archivedNote
            totalArchivedCount shouldBe 1 // only archivedNote across all users
        }

        @Test
        fun `search methods should find notes by title and content`() {
            // When
            val titleSearch =
                noteRepository
                    .findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                        testUser1,
                        "test",
                        "test",
                    )
            val contentSearch =
                noteRepository
                    .findAllByUserAndArchivedFalseAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(
                        testUser1,
                        "different",
                        "different",
                    )

            // Then
            titleSearch shouldHaveSize 1
            titleSearch shouldContain note1

            contentSearch shouldHaveSize 1
            contentSearch shouldContain note2
        }

        @Test
        fun `findAllByUserAndArchivedFalse with sort should respect sorting`() {
            // When
            val sortedByTitle =
                noteRepository.findAllByUserAndArchivedFalse(
                    testUser1,
                    Sort.by(Sort.Direction.ASC, "title"),
                )

            // Then
            sortedByTitle shouldHaveSize 2
            sortedByTitle[0] shouldBe note2 // "Another Note" comes before "Test Note 1"
            sortedByTitle[1] shouldBe note1
        }

        @Test
        fun `findAllByUserAndArchivedFalseAndQuery with custom query should work`() {
            // When
            val searchResults =
                noteRepository.findAllByUserAndArchivedFalseAndQuery(
                    testUser1,
                    "test",
                    Sort.by(Sort.Direction.DESC, "updatedAt"),
                )

            // Then
            searchResults shouldHaveSize 1
            searchResults shouldContain note1
        }

        @Test
        fun `findAllAccessibleByUserAndArchivedFalse should include shared notes`() {
            // Given - create a shared note
            val sharedNote =
                noteRepository.save(
                    Note(
                        title = "Shared Note",
                        content = "This note is shared",
                        user = testUser2,
                    ),
                )
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser1,
                    sharedByUser = testUser2,
                    permission = "read",
                ),
            )

            // When
            val accessibleNotes =
                noteRepository.findAllAccessibleByUserAndArchivedFalse(
                    testUser1,
                    Sort.by(Sort.Direction.DESC, "updatedAt"),
                )

            // Then
            accessibleNotes shouldHaveSize 3 // note1, note2, sharedNote
            accessibleNotes shouldContain note1
            accessibleNotes shouldContain note2
            accessibleNotes shouldContain sharedNote
        }

        @Test
        fun `findByIdAndAccessibleByUser should find shared notes`() {
            // Given - create a shared note
            val sharedNote =
                noteRepository.save(
                    Note(
                        title = "Shared Note",
                        content = "This note is shared",
                        user = testUser2,
                    ),
                )
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser1,
                    sharedByUser = testUser2,
                    permission = "read",
                ),
            )

            // When
            val foundOwnNote = noteRepository.findByIdAndAccessibleByUser(note1.id!!, testUser1)
            val foundSharedNote = noteRepository.findByIdAndAccessibleByUser(sharedNote.id!!, testUser1)
            val notFoundNote = noteRepository.findByIdAndAccessibleByUser(sharedNote.id!!, testUser2)

            // Then
            foundOwnNote shouldBe note1
            foundSharedNote shouldBe sharedNote
            // testUser2 owns the note, so should also be accessible
            notFoundNote shouldBe sharedNote
        }

        @Test
        fun `findAllAccessibleByUserAndArchivedFalseAndQuery should search in shared notes`() {
            // Given - create a shared note with searchable content
            val sharedNote =
                noteRepository.save(
                    Note(
                        title = "Shared Test Note",
                        content = "This shared note contains test content",
                        user = testUser2,
                    ),
                )
            noteShareRepository.save(
                NoteShare(
                    note = sharedNote,
                    sharedWithUser = testUser1,
                    sharedByUser = testUser2,
                    permission = "read",
                ),
            )

            // When
            val searchResults =
                noteRepository.findAllAccessibleByUserAndArchivedFalseAndQuery(
                    testUser1,
                    "test",
                    Sort.by(Sort.Direction.DESC, "updatedAt"),
                )

            // Then
            searchResults shouldHaveSize 2 // note1 and sharedNote both contain "test"
            searchResults shouldContain note1
            searchResults shouldContain sharedNote
        }

        @Test
        fun `repository should handle empty results gracefully`() {
            // When
            val emptyResults = noteRepository.findAllByUserAndArchivedFalseOrderByUpdatedAtDesc(testUser2)
            val emptySearch =
                noteRepository.findAllByUserAndArchivedFalseAndQuery(
                    testUser2,
                    "nonexistent",
                    Sort.by(Sort.Direction.DESC, "updatedAt"),
                )

            // Then
            emptyResults shouldHaveSize 0
            emptySearch shouldHaveSize 0
        }
    }
