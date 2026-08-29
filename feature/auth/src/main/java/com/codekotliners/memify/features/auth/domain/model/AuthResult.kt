package com.codekotliners.memify.features.auth.domain.model

internal sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(
        val reason: AuthFailure,
    ) : AuthResult
}

internal sealed interface AuthFailure {
    data object Network : AuthFailure

    data object InvalidCredentials : AuthFailure

    data object EmailAlreadyUsed : AuthFailure

    data object GoogleRejected : AuthFailure

    data object RequestRejected : AuthFailure

    data object ServiceUnavailable : AuthFailure

    data object Unknown : AuthFailure
}
