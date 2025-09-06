package com.poisonedyouth.nota.admin

import com.poisonedyouth.nota.notes.NoteRepository
import com.poisonedyouth.nota.notes.NoteShareRepository
import com.poisonedyouth.nota.user.User
import com.poisonedyouth.nota.user.UserRepository
import com.poisonedyouth.nota.user.UserRole
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

class AdminServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var noteRepository: NoteRepository
    private lateinit var noteShareRepository: NoteShareRepository
    private lateinit var adminService: AdminService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        noteRepository = mockk()
        noteShareRepository = mockk()
        adminService = AdminService(userRepository, noteRepository, noteShareRepository, java.time.Clock.systemDefaultZone())
    }

    @Test
    fun `getAllUserStatistics should return statistics for all users`() {
        // Given
        val user1 =
            User(
                id = 1L,
                username = "user1",
                password = "hashedPassword1",
                role = UserRole.USER,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                enabled = true,
            )
        val user2 =
            User(
                id = 2L,
                username = "testuser",
                password = "hashedPassword2",
                mustChangePassword = true,
                role = UserRole.ADMIN,
                createdAt = LocalDateTime.of(2024, 1, 2, 11, 0),
                updatedAt = LocalDateTime.of(2024, 1, 2, 11, 0),
                enabled = true,
            )
        val users = listOf(user1, user2)

        every { userRepository.findAll() } returns users
        every { noteRepository.countByUser(user1) } returns 5L
        every { noteRepository.countByUserAndArchivedTrue(user1) } returns 2L
        every { noteShareRepository.countBySharedByUser(user1) } returns 1L
        every { noteRepository.countByUser(user2) } returns 0L
        every { noteRepository.countByUserAndArchivedTrue(user2) } returns 0L
        every { noteShareRepository.countBySharedByUser(user2) } returns 0L

        // When
        val result = adminService.getAllUserStatistics()

        // Then
        result.size shouldBe 2

        val user1Stats = result.find { it.username == "user1" }!!
        user1Stats.id shouldBe 1L
        user1Stats.username shouldBe "user1"
        user1Stats.totalNotes shouldBe 5L
        user1Stats.archivedNotes shouldBe 2L
        user1Stats.sharedNotes shouldBe 1L
        user1Stats.mustChangePassword shouldBe false
        user1Stats.createdAt shouldBe LocalDateTime.of(2024, 1, 1, 10, 0)

        val user2Stats = result.find { it.username == "testuser" }!!
        user2Stats.id shouldBe 2L
        user2Stats.username shouldBe "testuser"
        user2Stats.totalNotes shouldBe 0L
        user2Stats.archivedNotes shouldBe 0L
        user2Stats.sharedNotes shouldBe 0L
        user2Stats.mustChangePassword shouldBe true
        user2Stats.createdAt shouldBe LocalDateTime.of(2024, 1, 2, 11, 0)

        verify { userRepository.findAll() }
        verify { noteRepository.countByUser(user1) }
        verify { noteRepository.countByUserAndArchivedTrue(user1) }
        verify { noteShareRepository.countBySharedByUser(user1) }
        verify { noteRepository.countByUser(user2) }
        verify { noteRepository.countByUserAndArchivedTrue(user2) }
        verify { noteShareRepository.countBySharedByUser(user2) }
    }

    @Test
    fun `getAllUserStatistics should return empty list when no users exist`() {
        // Given
        every { userRepository.findAll() } returns emptyList()

        // When
        val result = adminService.getAllUserStatistics()

        // Then
        result shouldBe emptyList()
        verify { userRepository.findAll() }
    }

    @Test
    fun `isAdmin should return true for user with ADMIN role`() {
        // Given
        val adminUser =
            User(
                id = 1L,
                username = "testuser",
                password = "hashedPassword",
                role = UserRole.ADMIN,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                enabled = true,
            )
        every { userRepository.findByUsername("testuser") } returns adminUser

        // When & Then
        adminService.isAdmin("testuser") shouldBe true
        verify { userRepository.findByUsername("testuser") }
    }

    @Test
    fun `isAdmin should return false for user with USER role`() {
        // Given
        val regularUser =
            User(
                id = 2L,
                username = "user1",
                password = "hashedPassword",
                role = UserRole.USER,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                enabled = true,
            )
        every { userRepository.findByUsername("user1") } returns regularUser

        // When & Then
        adminService.isAdmin("user1") shouldBe false
        verify { userRepository.findByUsername("user1") }
    }

    @Test
    fun `isAdmin should return false for non-existent user`() {
        // Given
        every { userRepository.findByUsername("nonexistent") } returns null

        // When & Then
        adminService.isAdmin("nonexistent") shouldBe false
        verify { userRepository.findByUsername("nonexistent") }
    }

    @Test
    fun `getSystemStatistics should return correct system statistics`() {
        // Given
        every { userRepository.count() } returns 10L
        every { noteRepository.count() } returns 50L
        every { noteRepository.countByArchivedTrue() } returns 15L
        every { noteShareRepository.count() } returns 8L

        // When
        val result = adminService.getSystemStatistics()

        // Then
        result.totalUsers shouldBe 10L
        result.totalNotes shouldBe 50L
        result.totalArchivedNotes shouldBe 15L
        result.totalSharedNotes shouldBe 8L

        verify { userRepository.count() }
        verify { noteRepository.count() }
        verify { noteRepository.countByArchivedTrue() }
        verify { noteShareRepository.count() }
    }

    @Test
    fun `getSystemStatistics should handle zero counts`() {
        // Given
        every { userRepository.count() } returns 0L
        every { noteRepository.count() } returns 0L
        every { noteRepository.countByArchivedTrue() } returns 0L
        every { noteShareRepository.count() } returns 0L

        // When
        val result = adminService.getSystemStatistics()

        // Then
        result.totalUsers shouldBe 0L
        result.totalNotes shouldBe 0L
        result.totalArchivedNotes shouldBe 0L
        result.totalSharedNotes shouldBe 0L

        verify { userRepository.count() }
        verify { noteRepository.count() }
        verify { noteRepository.countByArchivedTrue() }
        verify { noteShareRepository.count() }
    }

    @Test
    fun `disableUser should disable regular user successfully`() {
        // Given
        val userId = 1L
        val user =
            User(
                id = userId,
                username = "testuser",
                password = "hashedPassword",
                role = UserRole.USER,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                enabled = true,
            )
        val userSlot = slot<User>()

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(capture(userSlot)) } returns user

        // When
        val result = adminService.disableUser(userId)

        // Then
        result shouldBe true
        val savedUser = userSlot.captured
        savedUser.enabled shouldBe false
        savedUser.username shouldBe "testuser"
        savedUser.role shouldBe UserRole.USER

        verify { userRepository.findById(userId) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `disableUser should not disable admin user`() {
        // Given
        val userId = 1L
        val adminUser =
            User(
                id = userId,
                username = "admin",
                password = "hashedPassword",
                role = UserRole.ADMIN,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                enabled = true,
            )

        every { userRepository.findById(userId) } returns Optional.of(adminUser)

        // When
        val result = adminService.disableUser(userId)

        // Then
        result shouldBe false

        verify { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `disableUser should return false for non-existent user`() {
        // Given
        val userId = 999L
        every { userRepository.findById(userId) } returns Optional.empty()

        // When
        val result = adminService.disableUser(userId)

        // Then
        result shouldBe false

        verify { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `enableUser should enable disabled user successfully`() {
        // Given
        val userId = 1L
        val user =
            User(
                id = userId,
                username = "testuser",
                password = "hashedPassword",
                role = UserRole.USER,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 10, 0),
                enabled = false,
            )
        val userSlot = slot<User>()

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(capture(userSlot)) } returns user

        // When
        val result = adminService.enableUser(userId)

        // Then
        result shouldBe true
        val savedUser = userSlot.captured
        savedUser.enabled shouldBe true
        savedUser.username shouldBe "testuser"
        savedUser.role shouldBe UserRole.USER

        verify { userRepository.findById(userId) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `enableUser should return false for non-existent user`() {
        // Given
        val userId = 999L
        every { userRepository.findById(userId) } returns Optional.empty()

        // When
        val result = adminService.enableUser(userId)

        // Then
        result shouldBe false

        verify { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
