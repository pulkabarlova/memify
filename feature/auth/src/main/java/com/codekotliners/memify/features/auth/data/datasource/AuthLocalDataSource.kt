package com.codekotliners.memify.features.auth.data.datasource

import com.codekotliners.memify.core.prefs.TokenStore
import com.codekotliners.memify.features.auth.data.model.AuthSessionData
import javax.inject.Inject

internal class AuthLocalDataSource @Inject constructor(
    private val tokenStore: TokenStore,
) {
    fun isAuthenticated(): Boolean = tokenStore.isLoggedIn()

    fun saveSession(session: AuthSessionData) {
        tokenStore.saveTokens(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            userId = session.userId,
        )
    }
}
