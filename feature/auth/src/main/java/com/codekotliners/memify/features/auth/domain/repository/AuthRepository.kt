package com.codekotliners.memify.features.auth.domain.repository

import com.codekotliners.memify.features.auth.domain.model.AuthResult

internal interface AuthRepository {
    fun isAuthenticated(): Boolean

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun register(name: String, email: String, password: String): AuthResult

    suspend fun signInWithGoogle(idToken: String): AuthResult
}
