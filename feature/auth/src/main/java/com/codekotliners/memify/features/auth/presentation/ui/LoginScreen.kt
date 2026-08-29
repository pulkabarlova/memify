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
import androidx.compose.material.icons.filled.Lock
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
import com.codekotliners.memify.features.auth.presentation.model.LoginAction
import com.codekotliners.memify.features.auth.presentation.model.LoginUiState
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthFormCard
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthFormHeader
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthMessageBanner
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthPasswordField
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthPrimaryButton
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthTopBar
import com.codekotliners.memify.features.auth.presentation.ui.components.text
import com.codekotliners.memify.features.auth.presentation.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onLoginSucceeded: () -> Unit,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigation) {
        when (state.navigation) {
            AuthNavigation.Authenticated -> onLoginSucceeded()
            null -> return@LaunchedEffect
        }
        viewModel.onAction(LoginAction.NavigationHandled)
    }

    LoginScreenContent(
        state = state,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginUiState,
    onBackClick: () -> Unit,
    onAction: (LoginAction) -> Unit,
) {
    val emailError = state.emailError?.text()
    val passwordErrors = state.passwordErrors.map { error -> error.text() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AuthTopBar(
                title = stringResource(R.string.login_title),
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
            item(key = HEADER_KEY) {
                AuthFormHeader(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.login_header_title),
                    description = stringResource(R.string.login_header_description),
                )
            }

            state.message?.let { message ->
                item(key = MESSAGE_KEY) {
                    AuthMessageBanner(
                        message = message,
                        onDismiss = { onAction(LoginAction.MessageDismissed) },
                    )
                }
            }

            item(key = FORM_KEY) {
                LoginFormCard(
                    state = state,
                    emailError = emailError,
                    passwordErrors = passwordErrors,
                    focusManager = focusManager,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun LoginFormCard(
    state: LoginUiState,
    emailError: String?,
    passwordErrors: List<String>,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (LoginAction) -> Unit,
) {
    AuthFormCard {
        OutlinedTextField(
            value = state.email,
            onValueChange = { email -> onAction(LoginAction.EmailChanged(email)) },
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

        AuthPasswordField(
            value = state.password,
            label = stringResource(R.string.password_field),
            enabled = state.isFormEnabled,
            errorMessages = passwordErrors,
            imeAction = ImeAction.Done,
            onImeAction = { onAction(LoginAction.Submit) },
            onValueChange = { password -> onAction(LoginAction.PasswordChanged(password)) },
        )

        AuthPrimaryButton(
            text = stringResource(R.string.login_button),
            isLoading = state.isSubmitting,
            enabled = state.isFormEnabled,
            onClick = { onAction(LoginAction.Submit) },
        )
    }
}

@Preview(name = "Login light", showSystemUi = true)
@Preview(name = "Login dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    MemifyTheme {
        LoginScreenContent(
            state = LoginUiState(),
            onBackClick = {},
            onAction = {},
        )
    }
}

private const val HEADER_KEY = "login_header"
private const val MESSAGE_KEY = "login_message"
private const val FORM_KEY = "login_form"
