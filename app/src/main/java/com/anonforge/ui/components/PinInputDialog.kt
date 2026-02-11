package com.anonforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anonforge.R

/**
 * Dialog for PIN input with security features.
 *
 * Features:
 * - Masked input by default
 * - Show/hide toggle
 * - Attempt counter display
 * - Loading state during verification
 * - Auto-focus on display
 * - Memory wipe on dismiss (via DisposableEffect)
 */
@Composable
fun PinInputDialog(
    onDismiss: () -> Unit,
    onPinEntered: (CharArray) -> Unit,
    attemptsRemaining: Int = 3,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    minPinLength: Int = 4,
    maxPinLength: Int = 8
) {
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Security: Wipe PIN from memory when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            pin = ""
        }
    }

    // Auto-focus PIN field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = {
            // Security: PIN wipe handled by DisposableEffect.onDispose
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.pin_dialog_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { newValue ->
                        // Only allow digits and respect max length
                        if (newValue.length <= maxPinLength && newValue.all { it.isDigit() }) {
                            pin = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.pin_dialog_label)) },
                    placeholder = { Text("••••") },
                    visualTransformation = if (showPin) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pin.length >= minPinLength) {
                                val pinArray = pin.toCharArray()
                                pin = "" // Wipe immediately
                                onPinEntered(pinArray)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (showPin) {
                                    stringResource(R.string.pin_hide)
                                } else {
                                    stringResource(R.string.pin_show)
                                }
                            )
                        }
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Attempts remaining
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pin_attempts_remaining, attemptsRemaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (attemptsRemaining <= 2) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = {
                        if (pin.length >= minPinLength) {
                            val pinArray = pin.toCharArray()
                            pin = "" // Wipe immediately
                            onPinEntered(pinArray)
                        }
                    },
                    enabled = pin.length >= minPinLength && !isLoading
                ) {
                    Text(stringResource(R.string.pin_verify))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Security: PIN wipe handled by DisposableEffect.onDispose
                    onDismiss()
                },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for setting up a new PIN.
 */
@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (CharArray) -> Unit,
    isLoading: Boolean = false,
    minPinLength: Int = 4,
    maxPinLength: Int = 8
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Security: Wipe PINs from memory when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            pin = ""
            confirmPin = ""
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Local validation function to avoid lint false positive with lambda
    fun handleSubmit() {
        val validationError = validatePinSetup(
            pin = pin,
            confirmPin = confirmPin,
            minLength = minPinLength
        )
        if (validationError != null) {
            errorMessage = validationError
        } else {
            onPinSet(pin.toCharArray())
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Security: PIN wipe handled by DisposableEffect.onDispose
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.pin_setup_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // PIN input
                OutlinedTextField(
                    value = pin,
                    onValueChange = { newValue ->
                        if (newValue.length <= maxPinLength && newValue.all { it.isDigit() }) {
                            pin = newValue
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin_setup_enter)) },
                    visualTransformation = if (showPin) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm PIN input
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { newValue ->
                        if (newValue.length <= maxPinLength && newValue.all { it.isDigit() }) {
                            confirmPin = newValue
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin_setup_confirm)) },
                    visualTransformation = if (showPin) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSubmit() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pin_setup_hint, minPinLength, maxPinLength),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = { handleSubmit() },
                    enabled = pin.length >= minPinLength && !isLoading
                ) {
                    Text(stringResource(R.string.pin_setup_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Security: PIN wipe handled by DisposableEffect.onDispose
                    onDismiss()
                },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Validates PIN setup and returns error message or null if valid.
 */
private fun validatePinSetup(
    pin: String,
    confirmPin: String,
    minLength: Int
): String? {
    return when {
        pin.length < minLength -> "PIN must be at least $minLength digits"
        pin != confirmPin -> "PINs do not match"
        else -> null
    }
}