package com.codekotliners.memify.features.auth.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.codekotliners.memify.features.auth.R
import com.codekotliners.memify.features.auth.domain.model.ConfirmPasswordValidationError
import com.codekotliners.memify.features.auth.domain.model.EmailValidationError
import com.codekotliners.memify.features.auth.domain.model.NameValidationError
import com.codekotliners.memify.features.auth.domain.model.PasswordValidationError

@Composable
internal fun EmailValidationError.text(): String =
    stringResource(
        when (this) {
            EmailValidationError.Empty -> R.string.email_cannot_be_empty
            EmailValidationError.InvalidFormat -> R.string.invalid_email_format
        },
    )

@Composable
internal fun NameValidationError.text(): String =
    stringResource(
        when (this) {
            NameValidationError.Empty -> R.string.name_cannot_be_empty
            NameValidationError.TooShort -> R.string.name_too_short
        },
    )

@Composable
internal fun PasswordValidationError.text(): String =
    stringResource(
        when (this) {
            PasswordValidationError.Empty -> R.string.password_cannot_be_empty
            PasswordValidationError.TooShort -> R.string.not_less_than_8_symbols_requirement
            PasswordValidationError.MissingUppercase -> R.string.require_uppercase_character
            PasswordValidationError.MissingDigit -> R.string.require_numeric_character
            PasswordValidationError.MissingSpecial -> R.string.require_special_character
        },
    )

@Composable
internal fun ConfirmPasswordValidationError.text(): String =
    stringResource(
        when (this) {
            ConfirmPasswordValidationError.Empty -> R.string.confirm_password_cannot_be_empty
            ConfirmPasswordValidationError.Mismatch -> R.string.passwords_do_not_match
        },
    )
