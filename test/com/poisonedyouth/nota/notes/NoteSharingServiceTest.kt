package com.poisonedyouth.nota.notes

import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class NoteSharingServiceTest :
    FunSpec({

        val noteRepository = mockk<NoteRepository>()
        val userRepository = mockk<UserRepository>()
        val noteShareRepository = mockk<NoteShareRepository>()
        val noteService = NoteService(noteRepository, userRepository, noteShareRepository, java.time.Clock.systemDefaultZone())

        val testUser =
            User(
                id = 1L,
                username = "testuser",
                password = "password",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val targetUser =
            User(
                id = 2L,
                username = "targetuser",
                password = "password",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val testNote =
            Note(
                id = 1L,
                title = "Test Note",
                content = "Test content",
                user = testUser,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        context("shareNote") {
            test("should successfully share note with valid user") {
                // Given
                val shareDto = ShareNoteDto("targetuser", "read")
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { userRepository.findByUsername("targetuser") } returns targetUser
                every { noteShareRepository.existsByNoteAndSharedWithUser(testNote, targetUser) } returns false
                every { noteShareRepository.save(any<NoteShare>()) } returns mockk()

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe true
                verify { noteShareRepository.save(any<NoteShare>()) }
            }

            test("should fail when user not found") {
                // Given
                val shareDto = ShareNoteDto("targetuser", "read")
                every { userRepository.findById(1L) } returns Optional.empty()

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe false
            }

            test("should fail when note not found") {
                // Given
                val shareDto = ShareNoteDto("targetuser", "read")
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns null

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe false
            }

            test("should fail when target user not found") {
                // Given
                val shareDto = ShareNoteDto("nonexistent", "read")
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { userRepository.findByUsername("nonexistent") } returns null

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe false
            }

            test("should fail when trying to share with self") {
                // Given
                val shareDto = ShareNoteDto("testuser", "read")
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { userRepository.findByUsername("testuser") } returns testUser

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe false
            }

            test("should fail when note already shared with user") {
                // Given
                val shareDto = ShareNoteDto("targetuser", "read")
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { userRepository.findByUsername("targetuser") } returns targetUser
                every { noteShareRepository.existsByNoteAndSharedWithUser(testNote, targetUser) } returns true

                // When
                val result = noteService.shareNote(1L, shareDto, 1L)

                // Then
                result shouldBe false
            }
        }

        context("revokeNoteShare") {
            test("should successfully revoke share") {
                // Given
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { userRepository.findById(2L) } returns Optional.of(targetUser)
                every { noteShareRepository.deleteByNoteAndSharedWithUser(testNote, targetUser) } returns Unit

                // When
                val result = noteService.revokeNoteShare(1L, 2L, 1L)

                // Then
                result shouldBe true
                verify { noteShareRepository.deleteByNoteAndSharedWithUser(testNote, targetUser) }
            }

            test("should fail when user not found") {
                // Given
                every { userRepository.findById(1L) } returns Optional.empty()

                // When
                val result = noteService.revokeNoteShare(1L, 2L, 1L)

                // Then
                result shouldBe false
            }
        }

        context("getNoteShares") {
            test("should return shares for valid note") {
                // Given
                val noteShare =
                    NoteShare(
                        id = 1L,
                        note = testNote,
                        sharedWithUser = targetUser,
                        sharedByUser = testUser,
                        permission = "read",
                        createdAt = LocalDateTime.now(),
                    )
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndUser(1L, testUser) } returns testNote
                every { noteShareRepository.findAllByNote(testNote) } returns listOf(noteShare)

                // When
                val result = noteService.getNoteShares(1L, 1L)

                // Then
                result shouldHaveSize 1
                result[0].sharedWithUsername shouldBe "targetuser"
            }

            test("should return empty list when user not found") {
                // Given
                every { userRepository.findById(1L) } returns Optional.empty()

                // When
                val result = noteService.getNoteShares(1L, 1L)

                // Then
                result.shouldBeEmpty()
            }
        }

        context("findAllSharedNotes") {
            test("should return shared notes for user") {
                // Given
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteShareRepository.findAllSharedWithUser(testUser) } returns listOf(testNote)

                // When
                val result = noteService.findAllSharedNotes(1L)

                // Then
                result shouldHaveSize 1
                result[0].title shouldBe "Test Note"
            }
        }

        context("canUserAccessNote") {
            test("should return true when user can access note") {
                // Given
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndAccessibleByUser(1L, testUser) } returns testNote

                // When
                val result = noteService.canUserAccessNote(1L, 1L)

                // Then
                result shouldBe true
            }

            test("should return false when user cannot access note") {
                // Given
                every { userRepository.findById(1L) } returns Optional.of(testUser)
                every { noteRepository.findByIdAndAccessibleByUser(1L, testUser) } returns null

                // When
                val result = noteService.canUserAccessNote(1L, 1L)

                // Then
                result shouldBe false
            }
        }
    })
