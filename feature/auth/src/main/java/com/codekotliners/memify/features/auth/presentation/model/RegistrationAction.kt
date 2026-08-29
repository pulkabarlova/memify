package com.codekotliners.memify.features.auth.presentation.model

internal sealed interface RegistrationAction {
    data class NameChanged(
        val name: String,
    ) : RegistrationAction

    data class EmailChanged(
        val email: String,
    ) : RegistrationAction

    data class PasswordChanged(
        val password: String,
    ) : RegistrationAction

    data class ConfirmPasswordChanged(
        val confirmation: String,
    ) : RegistrationAction

    data object Submit : RegistrationAction

    data object MessageDismissed : RegistrationAction

    data object NavigationHandled : RegistrationAction
}
