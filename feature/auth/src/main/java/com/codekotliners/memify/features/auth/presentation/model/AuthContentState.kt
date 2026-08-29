package com.codekotliners.memify.features.auth.presentation.model

internal sealed interface AuthContentState {
    data object CheckingSession : AuthContentState

    data object Content : AuthContentState

    data object Error : AuthContentState
}
