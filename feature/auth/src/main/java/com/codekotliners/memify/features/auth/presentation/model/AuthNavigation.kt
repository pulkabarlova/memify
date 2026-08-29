package com.codekotliners.memify.features.auth.presentation.model

internal sealed interface AuthNavigation {
    data object Authenticated : AuthNavigation
}
