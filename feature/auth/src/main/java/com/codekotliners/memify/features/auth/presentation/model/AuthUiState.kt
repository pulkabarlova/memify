package com.codekotliners.memify.features.auth.presentation.model

internal data class AuthUiState(
    val contentState: AuthContentState = AuthContentState.CheckingSession,
    val isGoogleSignInInProgress: Boolean = false,
    val message: AuthMessage? = null,
    val navigation: AuthNavigation? = null,
) {
    val isInteractionEnabled: Boolean
        get() = contentState == AuthContentState.Content && !isGoogleSignInInProgress
}
