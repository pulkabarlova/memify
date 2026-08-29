package com.codekotliners.memify.features.auth.presentation.model

import com.codekotliners.memify.features.auth.domain.model.ConfirmPasswordValidationError
import com.codekotliners.memify.features.auth.domain.model.EmailValidationError
import com.codekotliners.memify.features.auth.domain.model.NameValidationError
import com.codekotliners.memify.features.auth.domain.model.PasswordValidationError

internal data class RegistrationUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: NameValidationError? = null,
    val emailError: EmailValidationError? = null,
    val passwordErrors: List<PasswordValidationError> = emptyList(),
    val confirmPasswordError: ConfirmPasswordValidationError? = null,
    val isSubmitting: Boolean = false,
    val message: AuthMessage? = null,
    val navigation: AuthNavigation? = null,
) {
    val isFormEnabled: Boolean
        get() = !isSubmitting
}
