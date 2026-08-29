package com.codekotliners.memify.features.auth.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.codekotliners.memify.features.auth.R

@Composable
internal fun AuthPasswordField(
    value: String,
    label: String,
    enabled: Boolean,
    errorMessages: List<String>,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = errorMessages.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(
                enabled = enabled,
                onClick = { passwordVisible = !passwordVisible },
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (passwordVisible) R.drawable.visibility_off else R.drawable.visibility,
                        ),
                    contentDescription = stringResource(R.string.toggle_password_visibility),
                )
            }
        },
        supportingText =
            if (errorMessages.isEmpty()) {
                null
            } else {
                {
                    Column {
                        errorMessages.forEach { message -> Text(message) }
                    }
                }
            },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
            ),
    )
}
