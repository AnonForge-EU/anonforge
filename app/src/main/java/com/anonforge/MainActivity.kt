package com.anonforge

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.anonforge.core.deeplink.DeepLinkManager
import com.anonforge.navigation.AnonForgeNavGraph
import com.anonforge.ui.theme.AnonForgeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for AnonForge application.
 *
 * Security measures:
 * - FLAG_SECURE: Prevents screenshots and screen recording
 * - Biometric authentication flow via NavGraph
 *
 * Deep link handling (Skill 17):
 * - anonforge://donation/success - Donation completed successfully
 * - anonforge://donation/cancel - Donation cancelled by user
 *
 * Theme handling (Skill 18):
 * - Observes user theme preference via MainViewModel
 * - Applies theme at root level for immediate updates
 *
 * NOTE: Uses AppCompatActivity (extends FragmentActivity) instead of
 * ComponentActivity to support BiometricPrompt which requires FragmentActivity.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var deepLinkManager: DeepLinkManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SECURITY: Prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Handle deep link if app was launched via URI
        handleIntent(intent)

        setContent {
            // Observe theme mode from ViewModel (Skill 18)
            val themeMode by viewModel.themeMode.collectAsState()

            AnonForgeTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AnonForgeNavGraph(navController = navController)
                }
            }
        }
    }

    /**
     * Handle new intents when app is already running.
     * Required because launchMode="singleTop" in AndroidManifest.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Process incoming intent for deep links.
     * SECURITY: Only handles anonforge:// scheme.
     */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        deepLinkManager.handleDeepLink(uri)
    }
}