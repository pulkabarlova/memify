package com.codekotliners.memify.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekotliners.memify.features.auth.domain.model.AuthResult
import com.codekotliners.memify.features.auth.domain.repository.AuthRepository
import com.codekotliners.memify.features.auth.domain.validation.AuthInputValidator
import com.codekotliners.memify.features.auth.presentation.model.AuthNavigation
import com.codekotliners.memify.features.auth.presentation.model.RegistrationAction
import com.codekotliners.memify.features.auth.presentation.model.RegistrationUiState
import com.codekotliners.memify.features.auth.presentation.model.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RegistrationViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val validator: AuthInputValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.NameChanged ->
                _uiState.update { state ->
                    state.copy(name = action.name, nameError = null, message = null)
                }

            is RegistrationAction.EmailChanged ->
                _uiState.update { state ->
                    state.copy(email = action.email, emailError = null, message = null)
                }

            is RegistrationAction.PasswordChanged ->
                _uiState.update { state ->
                    state.copy(
                        password = action.password,
                        passwordErrors = emptyList(),
                        confirmPasswordError =
                            state.confirmPassword
                                .takeIf(String::isNotBlank)
                                ?.let { confirmation ->
                                    validator.validateConfirmPassword(action.password, confirmation)
                                },
                        message = null,
                    )
                }

            is RegistrationAction.ConfirmPasswordChanged ->
                _uiState.update { state ->
                    state.copy(
                        confirmPassword = action.confirmation,
                        confirmPasswordError = null,
                        message = null,
                    )
                }

            RegistrationAction.Submit -> submit()
            RegistrationAction.MessageDismissed -> _uiState.update { state -> state.copy(message = null) }
            RegistrationAction.NavigationHandled -> _uiState.update { state -> state.copy(navigation = null) }
        }
    }

    private fun submit() {
        val currentState = _uiState.value
        if (currentState.isSubmitting) return

        val validation =
            validator.validateRegistration(
                name = currentState.name,
                email = currentState.email,
                password = currentState.password,
                confirmation = currentState.confirmPassword,
            )
        val validatedState =
            currentState.copy(
                nameError = validation.nameError,
                emailError = validation.emailError,
                passwordErrors = validation.passwordErrors,
                confirmPasswordError = validation.confirmPasswordError,
                message = null,
            )
        _uiState.value = validatedState

        if (!validation.isValid) return

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSubmitting = true) }
            when (
                val result =
                    repository.register(
                        name = validation.name,
                        email = validation.email,
                        password = validation.password,
                    )
            ) {
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
