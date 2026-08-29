package com.codekotliners.memify.features.auth.data.model

import com.codekotliners.memify.core.network.models.AuthResponseDto

internal data class AuthSessionData(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
)

internal fun AuthResponseDto.toData(): AuthSessionData =
    AuthSessionData(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
    )
