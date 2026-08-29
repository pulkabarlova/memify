package com.codekotliners.memify.features.auth.presentation.model

import com.codekotliners.memify.features.auth.domain.model.EmailValidationError
import com.codekotliners.memify.features.auth.domain.model.PasswordValidationError

internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailValidationError? = null,
    val passwordErrors: List<PasswordValidationError> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: AuthMessage? = null,
    val navigation: AuthNavigation? = null,
) {
    val isFormEnabled: Boolean
        get() = !isSubmitting
}
