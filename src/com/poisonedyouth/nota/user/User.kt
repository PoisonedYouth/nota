package com.poisonedyouth.nota.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@Suppress("LongParameterList") // JPA entity with many fields
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true)
    val username: String,
    @Column(nullable = false)
    val password: String,
    @Column(nullable = false)
    val mustChangePassword: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    val enabled: Boolean = true,
)
