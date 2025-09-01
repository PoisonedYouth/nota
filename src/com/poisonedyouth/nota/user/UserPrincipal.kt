package com.poisonedyouth.nota.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Spring Security principal bridging our User entity to the SecurityContext.
 */
data class UserPrincipal(
    val id: Long,
    val usernameValue: String,
    private val passwordHash: String,
    private val role: UserRole,
    private val enabledValue: Boolean,
    val mustChangePassword: Boolean,
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val roleName = "ROLE_${role.name}"
        return mutableListOf(SimpleGrantedAuthority(roleName))
    }

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = usernameValue

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabledValue

    companion object {
        fun fromEntity(user: User): UserPrincipal {
            val id = user.id ?: -1L
            return UserPrincipal(
                id = id,
                usernameValue = user.username,
                passwordHash = user.password,
                role = user.role,
                enabledValue = user.enabled,
                mustChangePassword = user.mustChangePassword,
            )
        }
    }
}
