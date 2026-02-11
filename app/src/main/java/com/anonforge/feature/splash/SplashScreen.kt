package com.anonforge.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import kotlinx.coroutines.delay

/**
 * Splash screen shown on app launch.
 * Checks authentication state and routes accordingly:
 * - Disclaimer not accepted → DisclaimerScreen
 * - Biometric/PIN enabled → UnlockScreen
 * - Otherwise → Main (Vault)
 */
@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToUnlock: () -> Unit,
    onNavigateToDisclaimer: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Animation trigger state - read reactively by animateFloatAsState
    // Warning suppressed: Compose reads this value reactively through recomposition
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    var animationStarted by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        // Trigger fade-in animation
        animationStarted = true
        // Show splash for 1.5 seconds before checking state
        delay(1500)
        viewModel.checkInitialState()
    }

    // Handle navigation when state is ready
    LaunchedEffect(state.navigationTarget) {
        state.navigationTarget?.let { target ->
            when (target) {
                SplashNavigationTarget.DISCLAIMER -> onNavigateToDisclaimer()
                SplashNavigationTarget.UNLOCK -> onNavigateToUnlock()
                SplashNavigationTarget.MAIN -> onNavigateToMain()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alphaAnim)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

enum class SplashNavigationTarget {
    DISCLAIMER,
    UNLOCK,
    MAIN
}

data class SplashState(
    val isLoading: Boolean = true,
    val navigationTarget: SplashNavigationTarget? = null
)