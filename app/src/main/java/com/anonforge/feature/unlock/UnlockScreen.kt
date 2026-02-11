package com.anonforge.feature.unlock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.security.auth.AuthState
import com.anonforge.ui.components.LockoutDialog
import com.anonforge.ui.components.PinInputDialog
import com.anonforge.ui.components.SecureScreen
import kotlinx.coroutines.delay

/**
 * Unlock screen with biometric authentication and PIN fallback.
 *
 * Features:
 * - Auto-triggers biometric on load
 * - PIN fallback when biometric fails
 * - Lockout display with countdown
 * - Secure screen (FLAG_SECURE)
 * - Accessibility support
 */
@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate when authenticated
    LaunchedEffect(state.authState) {
        if (state.authState is AuthState.Authenticated) {
            onUnlocked()
        }
    }

    // Show error messages via snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SKILL 15.3: Connect viewModel.triggerBiometric() - previously bypassed
    // Trigger biometric prompt when ViewModel signals shouldTryBiometric
    // ═══════════════════════════════════════════════════════════════════════════
    LaunchedEffect(state.shouldTryBiometric) {
        if (state.shouldTryBiometric && activity != null) {
            delay(300) // Brief delay for UI to settle
            showBiometricPrompt(
                activity = activity,
                viewModel = viewModel,
                title = context.getString(R.string.biometric_prompt_title),
                subtitle = context.getString(R.string.biometric_prompt_subtitle),
                negativeButton = context.getString(R.string.biometric_use_pin)
            )
        }
    }

    // Countdown timer for lockout state
    // Uses while loop to continuously refresh until no longer locked out
    LaunchedEffect(state.authState) {
        while (state.authState is AuthState.LockedOut) {
            delay(1000)
            viewModel.refreshLockoutTimer()
        }
    }

    SecureScreen {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Lock icon with animation
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.unlock_icon_description),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = stringResource(R.string.unlock_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = stringResource(R.string.unlock_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Biometric authentication button
                    if (state.biometricAvailable) {
                        Button(
                            onClick = {
                                // Use ViewModel to trigger biometric (proper architecture)
                                viewModel.triggerBiometric()
                            },
                            modifier = Modifier
                                .size(width = 220.dp, height = 56.dp)
                                .semantics {
                                    contentDescription = context.getString(R.string.unlock_biometric_button_description)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.unlock_use_biometric))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // PIN authentication button
                    if (state.pinAvailable) {
                        val buttonModifier = Modifier
                            .size(width = 220.dp, height = 56.dp)
                            .semantics {
                                contentDescription = context.getString(R.string.unlock_pin_button_description)
                            }

                        if (state.biometricAvailable) {
                            // Secondary style when biometric is primary
                            OutlinedButton(
                                onClick = { viewModel.showPinDialog() },
                                modifier = buttonModifier
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.unlock_use_pin))
                            }
                        } else {
                            // Primary style when PIN is the only option
                            Button(
                                onClick = { viewModel.showPinDialog() },
                                modifier = buttonModifier
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.unlock_use_pin))
                            }
                        }
                    }

                    // No authentication methods configured
                    if (!state.biometricAvailable && !state.pinAvailable) {
                        Text(
                            text = stringResource(R.string.unlock_no_auth_configured),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // PIN input dialog
        if (state.showPinDialog) {
            PinInputDialog(
                onDismiss = { viewModel.dismissPinDialog() },
                onPinEntered = { pin -> viewModel.verifyPin(pin) },
                attemptsRemaining = state.attemptsRemaining,
                isLoading = state.isVerifyingPin,
                errorMessage = state.pinError
            )
        }

        // Lockout dialog with countdown
        val lockedOutState = state.authState as? AuthState.LockedOut
        if (state.showLockoutDialog && lockedOutState != null) {
            LockoutDialog(
                remainingSeconds = lockedOutState.remainingSeconds,
                onDismiss = {
                    viewModel.dismissLockoutDialog()
                    // Close app when locked out
                    (context as? Activity)?.finishAffinity()
                }
            )
        }
    }
}

/**
 * Helper to extract FragmentActivity from Context.
 * Necessary because LocalContext.current may return a ContextWrapper,
 * not the Activity directly. This unwraps the chain to find the actual Activity.
 */
private fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Show biometric authentication prompt.
 * Creates BiometricPrompt via AuthManager and shows system dialog.
 */
private fun showBiometricPrompt(
    activity: FragmentActivity,
    viewModel: UnlockViewModel,
    title: String,
    subtitle: String,
    negativeButton: String
) {
    val authManager = viewModel.getAuthManager()

    val prompt = authManager.createBiometricPrompt(
        activity = activity,
        onSuccess = { viewModel.onBiometricSuccess() },
        onError = { code, message -> viewModel.onBiometricError(code, message) },
        onFailed = { viewModel.onBiometricFailed() }
    )

    val promptInfo = authManager.createPromptInfo(
        title = title,
        subtitle = subtitle,
        negativeButtonText = negativeButton
    )

    prompt.authenticate(promptInfo)
}
