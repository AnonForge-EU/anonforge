package com.anonforge.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Wrapper composable that enables FLAG_SECURE to prevent screenshots.
 * 
 * Security feature: Prevents screen capture and recording while
 * displaying sensitive authentication UI.
 * 
 * Usage:
 * ```
 * SecureScreen {
 *     // Your sensitive content here
 *     UnlockContent()
 * }
 * ```
 */
@Composable
fun SecureScreen(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    DisposableEffect(Unit) {
        // Enable FLAG_SECURE when composable enters composition
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        onDispose {
            // Note: We don't clear FLAG_SECURE here because MainActivity
            // sets it globally. If you need per-screen control, uncomment:
            // activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    
    content()
}
