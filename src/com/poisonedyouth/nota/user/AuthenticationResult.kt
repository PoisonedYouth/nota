package com.poisonedyouth.nota.user

sealed class AuthenticationResult {
    data class Success(val user: UserDto) : AuthenticationResult()
    data object InvalidCredentials : AuthenticationResult()
    data object UserDisabled : AuthenticationResult()
}
