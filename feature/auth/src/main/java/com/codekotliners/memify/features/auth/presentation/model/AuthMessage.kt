package com.codekotliners.memify.features.auth.presentation.model

import com.codekotliners.memify.features.auth.domain.model.AuthFailure

internal sealed interface AuthMessage {
    data object NetworkUnavailable : AuthMessage

    data object InvalidCredentials : AuthMessage

    data object EmailAlreadyUsed : AuthMessage

    data object GoogleRejected : AuthMessage

    data object RequestRejected : AuthMessage

    data object ServiceUnavailable : AuthMessage

    data object GoogleSignInFailed : AuthMessage

    data object Unknown : AuthMessage
}

internal fun AuthFailure.toUiMessage(): AuthMessage =
    when (this) {
        AuthFailure.Network -> AuthMessage.NetworkUnavailable
        AuthFailure.InvalidCredentials -> AuthMessage.InvalidCredentials
        AuthFailure.EmailAlreadyUsed -> AuthMessage.EmailAlreadyUsed
        AuthFailure.GoogleRejected -> AuthMessage.GoogleRejected
        AuthFailure.RequestRejected -> AuthMessage.RequestRejected
        AuthFailure.ServiceUnavailable -> AuthMessage.ServiceUnavailable
        AuthFailure.Unknown -> AuthMessage.Unknown
    }
