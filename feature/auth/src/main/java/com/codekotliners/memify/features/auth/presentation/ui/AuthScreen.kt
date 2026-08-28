package com.codekotliners.memify.features.auth.presentation.ui

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekotliners.memify.core.logger.Logger
import com.codekotliners.memify.core.theme.MemifyTheme
import com.codekotliners.memify.features.auth.R
import com.codekotliners.memify.features.auth.presentation.viewmodel.AuthState
import com.codekotliners.memify.features.auth.presentation.viewmodel.AuthenticationViewModel
import com.google.android.gms.common.api.ApiException as GoogleApiException

@Composable
fun AuthScreen(
    branchAuthenticationCompleted: Boolean,
    onBranchAuthenticationHandled: () -> Unit,
    onAuthenticated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(branchAuthenticationCompleted) {
        if (branchAuthenticationCompleted) {
            viewModel.checkCurrentUser()
            onBranchAuthenticationHandled()
        }
    }

    val googleLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.handleGoogleSignInResult(result)
        }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onAuthenticated()
                viewModel.resetSignInState()
            }
            is AuthState.Error -> {
                showError(context, (authState as AuthState.Error).exception)
                viewModel.resetSignInState()
            }
            is AuthState.Loading -> {}
            is AuthState.Unauthenticated -> {}
        }
    }

    if (authState == AuthState.Unauthenticated) {
        AuthScreenContent(
            webClientId = viewModel.webClientId,
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister,
            onGoogleLauncherClick = {
                googleLauncher.launch(viewModel.getGoogleSignInIntent())
            },
            onLogInWithGoogle = { tokenId -> viewModel.onLogInWithGoogle(tokenId) },
        )
    } else {
        LoaderScreen()
    }
}

private fun showError(context: Context, error: Throwable) {
    val message =
        if (error is GoogleApiException) {
            context.getString(R.string.login_error_google_code, error.statusCode)
        } else {
            context.getString(R.string.login_error_message)
        }

    Logger.logError(message, error)
    Toast
        .makeText(
            context,
            message,
            Toast.LENGTH_LONG,
        ).show()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showSystemUi = true)
@Composable
fun AuthScreenPreview() {
    MemifyTheme {
        AuthScreen(
            branchAuthenticationCompleted = false,
            onBranchAuthenticationHandled = {},
            onAuthenticated = {},
            onNavigateToLogin = {},
            onNavigateToRegister = {},
        )
    }
}
