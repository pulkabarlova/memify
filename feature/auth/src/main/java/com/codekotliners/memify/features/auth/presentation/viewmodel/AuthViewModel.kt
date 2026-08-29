package com.codekotliners.memify.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekotliners.memify.features.auth.di.GoogleWebClientId
import com.codekotliners.memify.features.auth.domain.model.AuthResult
import com.codekotliners.memify.features.auth.domain.repository.AuthRepository
import com.codekotliners.memify.features.auth.presentation.model.AuthAction
import com.codekotliners.memify.features.auth.presentation.model.AuthContentState
import com.codekotliners.memify.features.auth.presentation.model.AuthMessage
import com.codekotliners.memify.features.auth.presentation.model.AuthNavigation
import com.codekotliners.memify.features.auth.presentation.model.AuthUiState
import com.codekotliners.memify.features.auth.presentation.model.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    @param:GoogleWebClientId val googleWebClientId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun onAction(action: AuthAction) {
        when (action) {
            AuthAction.RetrySessionCheck,
            AuthAction.AuthenticationChanged,
            -> checkSession()

            AuthAction.GoogleSignInStarted -> startGoogleSignIn()
            is AuthAction.GoogleTokenReceived -> authenticateWithGoogle(action.idToken)
            AuthAction.GoogleSignInCancelled -> finishGoogleSignIn()
            AuthAction.GoogleSignInFailed -> failGoogleSignIn()
            AuthAction.MessageDismissed -> _uiState.update { state -> state.copy(message = null) }
            AuthAction.NavigationHandled -> _uiState.update { state -> state.copy(navigation = null) }
        }
    }

    private fun checkSession() {
        _uiState.update { state ->
            state.copy(
                contentState = AuthContentState.CheckingSession,
                isGoogleSignInInProgress = false,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                val authenticated = repository.isAuthenticated()
                _uiState.update { state ->
                    state.copy(
                        contentState = AuthContentState.Content,
                        navigation = if (authenticated) AuthNavigation.Authenticated else null,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { state -> state.copy(contentState = AuthContentState.Error) }
            }
        }
    }

    private fun startGoogleSignIn() {
        if (!_uiState.value.isInteractionEnabled) return
        _uiState.update { state ->
            state.copy(
                isGoogleSignInInProgress = true,
                message = null,
            )
        }
    }

    private fun authenticateWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            failGoogleSignIn()
            return
        }

        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(idToken)) {
                AuthResult.Success ->
                    _uiState.update { state ->
                        state.copy(
                            isGoogleSignInInProgress = false,
                            message = null,
                            navigation = AuthNavigation.Authenticated,
                        )
                    }

                is AuthResult.Failure ->
                    _uiState.update { state ->
                        state.copy(
                            isGoogleSignInInProgress = false,
                            message = result.reason.toUiMessage(),
                        )
                    }
            }
        }
    }

    private fun finishGoogleSignIn() {
        _uiState.update { state -> state.copy(isGoogleSignInInProgress = false) }
    }

    private fun failGoogleSignIn() {
        _uiState.update { state ->
            state.copy(
                isGoogleSignInInProgress = false,
                message = AuthMessage.GoogleSignInFailed,
            )
        }
    }
}
