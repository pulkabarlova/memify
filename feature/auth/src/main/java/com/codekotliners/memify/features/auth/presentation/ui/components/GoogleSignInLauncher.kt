package com.codekotliners.memify.features.auth.presentation.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.codekotliners.memify.core.logger.Logger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

internal sealed interface GoogleSignInOutcome {
    data class Token(
        val idToken: String,
    ) : GoogleSignInOutcome

    data object Cancelled : GoogleSignInOutcome

    data object Failed : GoogleSignInOutcome
}

@Composable
internal fun rememberGoogleSignInLauncher(
    webClientId: String,
    onResult: (GoogleSignInOutcome) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val signInClient =
        remember(context, webClientId) {
            val options =
                GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
            GoogleSignIn.getClient(context, options)
        }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED && result.data == null) {
                onResult(GoogleSignInOutcome.Cancelled)
                return@rememberLauncherForActivityResult
            }

            try {
                val account =
                    GoogleSignIn
                        .getSignedInAccountFromIntent(result.data)
                        .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    onResult(GoogleSignInOutcome.Failed)
                } else {
                    onResult(GoogleSignInOutcome.Token(idToken))
                }
            } catch (exception: ApiException) {
                if (exception.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    onResult(GoogleSignInOutcome.Cancelled)
                } else {
                    Logger.logError("Google Sign-In failed", exception)
                    onResult(GoogleSignInOutcome.Failed)
                }
            }
        }

    return remember(signInClient, launcher) {
        { launcher.launch(signInClient.signInIntent) }
    }
}
