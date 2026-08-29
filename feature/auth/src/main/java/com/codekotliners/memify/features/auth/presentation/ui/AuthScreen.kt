package com.codekotliners.memify.features.auth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codekotliners.memify.features.auth.presentation.model.AuthAction
import com.codekotliners.memify.features.auth.presentation.model.AuthNavigation
import com.codekotliners.memify.features.auth.presentation.ui.components.AuthLandingContent
import com.codekotliners.memify.features.auth.presentation.ui.components.GoogleSignInOutcome
import com.codekotliners.memify.features.auth.presentation.ui.components.rememberGoogleSignInLauncher
import com.codekotliners.memify.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    branchAuthenticationCompleted: Boolean,
    onBranchAuthenticationHandled: () -> Unit,
    onAuthenticated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchGoogleSignIn =
        rememberGoogleSignInLauncher(viewModel.googleWebClientId) { outcome ->
            viewModel.onAction(
                when (outcome) {
                    is GoogleSignInOutcome.Token -> AuthAction.GoogleTokenReceived(outcome.idToken)
                    GoogleSignInOutcome.Cancelled -> AuthAction.GoogleSignInCancelled
                    GoogleSignInOutcome.Failed -> AuthAction.GoogleSignInFailed
                },
            )
        }

    LaunchedEffect(branchAuthenticationCompleted) {
        if (branchAuthenticationCompleted) {
            onBranchAuthenticationHandled()
            viewModel.onAction(AuthAction.AuthenticationChanged)
        }
    }

    LaunchedEffect(state.navigation) {
        when (state.navigation) {
            AuthNavigation.Authenticated -> onAuthenticated()
            null -> return@LaunchedEffect
        }
        viewModel.onAction(AuthAction.NavigationHandled)
    }

    AuthLandingContent(
        state = state,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToRegister = onNavigateToRegister,
        onGoogleClick = {
            viewModel.onAction(AuthAction.GoogleSignInStarted)
            launchGoogleSignIn()
        },
        onRetry = { viewModel.onAction(AuthAction.RetrySessionCheck) },
        onMessageDismiss = { viewModel.onAction(AuthAction.MessageDismissed) },
    )
}
