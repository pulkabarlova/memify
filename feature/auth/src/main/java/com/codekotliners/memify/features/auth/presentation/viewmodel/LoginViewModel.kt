package com.codekotliners.memify.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekotliners.memify.features.auth.domain.model.AuthResult
import com.codekotliners.memify.features.auth.domain.repository.AuthRepository
import com.codekotliners.memify.features.auth.domain.validation.AuthInputValidator
import com.codekotliners.memify.features.auth.presentation.model.AuthNavigation
import com.codekotliners.memify.features.auth.presentation.model.LoginAction
import com.codekotliners.memify.features.auth.presentation.model.LoginUiState
import com.codekotliners.memify.features.auth.presentation.model.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val validator: AuthInputValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged ->
                _uiState.update { state ->
                    state.copy(
                        email = action.email,
                        emailError = null,
                        message = null,
                    )
                }

            is LoginAction.PasswordChanged ->
                _uiState.update { state ->
                    state.copy(
                        password = action.password,
                        passwordErrors = emptyList(),
                        message = null,
                    )
                }

            LoginAction.Submit -> submit()
            LoginAction.MessageDismissed -> _uiState.update { state -> state.copy(message = null) }
            LoginAction.NavigationHandled -> _uiState.update { state -> state.copy(navigation = null) }
        }
    }

    private fun submit() {
        val currentState = _uiState.value
        if (currentState.isSubmitting) return

        val validation = validator.validateLogin(currentState.email, currentState.password)
        val validatedState =
            currentState.copy(
                emailError = validation.emailError,
                passwordErrors = validation.passwordErrors,
                message = null,
            )
        _uiState.value = validatedState

        if (!validation.isValid) return

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSubmitting = true) }
            when (val result = repository.signIn(validation.email, validation.password)) {
                AuthResult.Success ->
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            navigation = AuthNavigation.Authenticated,
                        )
                    }

                is AuthResult.Failure ->
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            message = result.reason.toUiMessage(),
                        )
                    }
            }
        }
    }
}
