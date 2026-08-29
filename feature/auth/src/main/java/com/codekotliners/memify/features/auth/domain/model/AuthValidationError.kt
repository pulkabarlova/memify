package com.codekotliners.memify.features.auth.domain.model

internal sealed interface EmailValidationError {
    data object Empty : EmailValidationError

    data object InvalidFormat : EmailValidationError
}

internal sealed interface NameValidationError {
    data object Empty : NameValidationError

    data object TooShort : NameValidationError
}

internal sealed interface PasswordValidationError {
    data object Empty : PasswordValidationError

    data object TooShort : PasswordValidationError

    data object MissingUppercase : PasswordValidationError

    data object MissingDigit : PasswordValidationError

    data object MissingSpecial : PasswordValidationError
}

internal sealed interface ConfirmPasswordValidationError {
    data object Empty : ConfirmPasswordValidationError

    data object Mismatch : ConfirmPasswordValidationError
}

internal data class LoginValidation(
    val email: String,
    val password: String,
    val emailError: EmailValidationError?,
    val passwordErrors: List<PasswordValidationError>,
) {
    val isValid: Boolean
        get() = emailError == null && passwordErrors.isEmpty()
}

internal data class RegistrationValidation(
    val name: String,
    val email: String,
    val password: String,
    val nameError: NameValidationError?,
    val emailError: EmailValidationError?,
    val passwordErrors: List<PasswordValidationError>,
    val confirmPasswordError: ConfirmPasswordValidationError?,
) {
    val isValid: Boolean
        get() =
            nameError == null &&
                emailError == null &&
                passwordErrors.isEmpty() &&
                confirmPasswordError == null
}
