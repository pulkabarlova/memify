package com.codekotliners.memify.features.auth.domain.validation

import com.codekotliners.memify.features.auth.domain.model.ConfirmPasswordValidationError
import com.codekotliners.memify.features.auth.domain.model.EmailValidationError
import com.codekotliners.memify.features.auth.domain.model.NameValidationError
import com.codekotliners.memify.features.auth.domain.model.PasswordValidationError
import com.codekotliners.memify.features.auth.domain.model.LoginValidation
import com.codekotliners.memify.features.auth.domain.model.RegistrationValidation
import javax.inject.Inject

internal class AuthInputValidator @Inject constructor() {
    fun validateLogin(email: String, password: String): LoginValidation =
        LoginValidation(
            email = email.trim(),
            password = password,
            emailError = validateEmail(email),
            passwordErrors = validateLoginPassword(password),
        )

    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        confirmation: String,
    ): RegistrationValidation =
        RegistrationValidation(
            name = name.trim(),
            email = email.trim(),
            password = password,
            nameError = validateName(name),
            emailError = validateEmail(email),
            passwordErrors = validateRegistrationPassword(password),
            confirmPasswordError = validateConfirmPassword(password, confirmation),
        )

    fun validateEmail(value: String): EmailValidationError? =
        when {
            value.isBlank() -> EmailValidationError.Empty
            !EMAIL_PATTERN.matches(value.trim()) -> EmailValidationError.InvalidFormat
            else -> null
        }

    fun validateName(value: String): NameValidationError? =
        when {
            value.isBlank() -> NameValidationError.Empty
            value.trim().length < MIN_NAME_LENGTH -> NameValidationError.TooShort
            else -> null
        }

    fun validateLoginPassword(value: String): List<PasswordValidationError> =
        if (value.isBlank()) {
            listOf(PasswordValidationError.Empty)
        } else {
            emptyList()
        }

    fun validateRegistrationPassword(value: String): List<PasswordValidationError> {
        if (value.isBlank()) return listOf(PasswordValidationError.Empty)

        return buildList {
            if (value.length < MIN_PASSWORD_LENGTH) add(PasswordValidationError.TooShort)
            if (value.none(Char::isUpperCase)) add(PasswordValidationError.MissingUppercase)
            if (value.none(Char::isDigit)) add(PasswordValidationError.MissingDigit)
            if (value.none { character -> !character.isLetterOrDigit() }) {
                add(PasswordValidationError.MissingSpecial)
            }
        }
    }

    fun validateConfirmPassword(
        password: String,
        confirmation: String,
    ): ConfirmPasswordValidationError? =
        when {
            confirmation.isBlank() -> ConfirmPasswordValidationError.Empty
            confirmation != password -> ConfirmPasswordValidationError.Mismatch
            else -> null
        }

    private companion object {
        const val MIN_NAME_LENGTH = 2
        const val MIN_PASSWORD_LENGTH = 8
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
