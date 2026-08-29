package com.codekotliners.memify.features.auth.presentation.model

internal sealed interface LoginAction {
    data class EmailChanged(
        val email: String,
    ) : LoginAction

    data class PasswordChanged(
        val password: String,
    ) : LoginAction

    data object Submit : LoginAction

    data object MessageDismissed : LoginAction

    data object NavigationHandled : LoginAction
}
