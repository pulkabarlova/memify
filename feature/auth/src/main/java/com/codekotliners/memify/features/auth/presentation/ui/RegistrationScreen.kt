package com.codekotliners.memify.features.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codekotliners.memify.core.theme.MemifyTheme
import com.codekotliners.memify.features.auth.R
import com.codekotliners.memify.features.auth.presentation.model.AuthNavigation
import com.codekotliners.memify.features.auth.presentation.model.RegistrationAction
import com.codekotliners.memify.features.auth.presentation.model.RegistrationUiState
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthFormCard
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthFormHeader
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthMessageBanner
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthPasswordField
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthPrimaryButton
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthTopBar
import com.codekotliners.memify.features.auth.presentation.ui.components.text
import com.codekotliners.memify.features.auth.presentation.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onBackClick: () -> Unit,
    onRegistrationSucceeded: () -> Unit,
) {
    val viewModel: RegistrationViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigation) {
        when (state.navigation) {
            AuthNavigation.Authenticated -> onRegistrationSucceeded()
            null -> return@LaunchedEffect
        }
        viewModel.onAction(RegistrationAction.NavigationHandled)
    }

    RegistrationScreenContent(
        state = state,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun RegistrationScreenContent(
    state: RegistrationUiState,
    onBackClick: () -> Unit,
    onAction: (RegistrationAction) -> Unit,
) {
    val nameError = state.nameError?.text()
    val emailError = state.emailError?.text()
    val passwordErrors = state.passwordErrors.map { error -> error.text() }
    val confirmationError = state.confirmPasswordError?.text()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AuthTopBar(
                title = stringResource(R.string.registration_title),
                onBackClick = onBackClick,
                enabled = state.isFormEnabled,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = REGISTRATION_HEADER_KEY) {
                AuthFormHeader(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.registration_header_title),
                    description = stringResource(R.string.registration_header_description),
                )
            }

            state.message?.let { message ->
                item(key = REGISTRATION_MESSAGE_KEY) {
                    AuthMessageBanner(
                        message = message,
                        onDismiss = { onAction(RegistrationAction.MessageDismissed) },
                    )
                }
            }

            item(key = REGISTRATION_FORM_KEY) {
                RegistrationFormCard(
                    state = state,
                    nameError = nameError,
                    emailError = emailError,
                    passwordErrors = passwordErrors,
                    confirmationError = confirmationError,
                    focusManager = focusManager,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun RegistrationFormCard(
    state: RegistrationUiState,
    nameError: String?,
    emailError: String?,
    passwordErrors: List<String>,
    confirmationError: String?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (RegistrationAction) -> Unit,
) {
    AuthFormCard {
        RegistrationIdentityFields(
            state = state,
            nameError = nameError,
            emailError = emailError,
            focusManager = focusManager,
            onAction = onAction,
        )
        RegistrationPasswordFields(
            state = state,
            passwordErrors = passwordErrors,
            confirmationError = confirmationError,
            focusManager = focusManager,
            onAction = onAction,
        )
        AuthPrimaryButton(
            text = stringResource(R.string.create_account_button),
            isLoading = state.isSubmitting,
            enabled = state.isFormEnabled,
            onClick = { onAction(RegistrationAction.Submit) },
        )
    }
}

@Composable
private fun RegistrationIdentityFields(
    state: RegistrationUiState,
    nameError: String?,
    emailError: String?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (RegistrationAction) -> Unit,
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { name -> onAction(RegistrationAction.NameChanged(name)) },
        enabled = state.isFormEnabled,
        isError = nameError != null,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.name_field)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
            )
        },
        supportingText = nameError?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
    )

    OutlinedTextField(
        value = state.email,
        onValueChange = { email -> onAction(RegistrationAction.EmailChanged(email)) },
        enabled = state.isFormEnabled,
        isError = emailError != null,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.email_field)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
            )
        },
        supportingText = emailError?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
    )
}

@Composable
private fun RegistrationPasswordFields(
    state: RegistrationUiState,
    passwordErrors: List<String>,
    confirmationError: String?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (RegistrationAction) -> Unit,
) {
    AuthPasswordField(
        value = state.password,
        label = stringResource(R.string.password_field),
        enabled = state.isFormEnabled,
        errorMessages = passwordErrors,
        imeAction = ImeAction.Next,
        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        onValueChange = { password -> onAction(RegistrationAction.PasswordChanged(password)) },
    )

    Text(
        text = stringResource(R.string.password_requirements_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )

    AuthPasswordField(
        value = state.confirmPassword,
        label = stringResource(R.string.password_confirmation_field),
        enabled = state.isFormEnabled,
        errorMessages = listOfNotNull(confirmationError),
        imeAction = ImeAction.Done,
        onImeAction = { onAction(RegistrationAction.Submit) },
        onValueChange = { confirmation ->
            onAction(RegistrationAction.ConfirmPasswordChanged(confirmation))
        },
    )
}

@Preview(name = "Registration light", showSystemUi = true)
@Preview(name = "Registration dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun RegistrationScreenPreview() {
    MemifyTheme {
        RegistrationScreenContent(
            state = RegistrationUiState(),
            onBackClick = {},
            onAction = {},
        )
    }
}

private const val REGISTRATION_HEADER_KEY = "registration_header"
private const val REGISTRATION_MESSAGE_KEY = "registration_message"
private const val REGISTRATION_FORM_KEY = "registration_form"
