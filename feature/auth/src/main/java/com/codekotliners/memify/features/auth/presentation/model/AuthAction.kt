package com.codekotliners.memify.features.auth.presentation.model

internal sealed interface AuthAction {
    data object RetrySessionCheck : AuthAction

    data object AuthenticationChanged : AuthAction

    data object GoogleSignInStarted : AuthAction

    data class GoogleTokenReceived(
        val idToken: String,
    ) : AuthAction

    data object GoogleSignInCancelled : AuthAction

    data object GoogleSignInFailed : AuthAction

    data object MessageDismissed : AuthAction

    data object NavigationHandled : AuthAction
}
