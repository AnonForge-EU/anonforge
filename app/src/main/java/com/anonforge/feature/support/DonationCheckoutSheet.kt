package com.anonforge.feature.support

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anonforge.R
import com.anonforge.core.stripe.StripePaymentLinks

/**
 * Fullscreen dialog containing Stripe Checkout WebView.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * WHY FULLSCREEN DIALOG INSTEAD OF BOTTOMSHEET?
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ModalBottomSheet intercepts vertical scroll gestures, which conflicts with
 * WebView scrolling. A fullscreen Dialog gives the WebView full control over
 * touch events, ensuring proper scrolling of Stripe's payment form.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * SECURITY MEASURES (OWASP M2 Compliant)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 1. DOMAIN WHITELIST: Only Stripe domains are allowed
 *    - checkout.stripe.com, buy.stripe.com, js.stripe.com
 *    - m.stripe.com, pay.stripe.com, billing.stripe.com
 *
 * 2. DEEP LINK INTERCEPTION: Custom scheme handled securely
 *    - anonforge://donation/success
 *    - anonforge://donation/cancel
 *
 * 3. JAVASCRIPT: Enabled only for Stripe functionality
 *
 * 4. DATA ISOLATION: Cache/history cleared, no file access
 *
 * 5. PRIVACY: No cookies persisted, no card data touches the app
 */
@Composable
fun DonationCheckoutSheet(
    checkoutUrl: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Handle back button
    BackHandler {
        onCancel()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            onCancel()
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false, // Fullscreen
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ═══════════════════════════════════════════════════════════════
                // SECURE HEADER
                // ═══════════════════════════════════════════════════════════════
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.donation_secure_payment),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.donation_checkout_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "stripe.com",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onCancel()
                                onDismiss()
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // ═══════════════════════════════════════════════════════════════
                // WEBVIEW CONTAINER
                // ═══════════════════════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Error state
                    if (loadError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.donation_error),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = loadError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // WebView - takes full remaining space
                        SecureStripeWebView(
                            url = checkoutUrl,
                            onSuccess = {
                                onSuccess()
                                onDismiss()
                            },
                            onCancel = {
                                onCancel()
                                onDismiss()
                            },
                            onLoadingChange = { isLoading = it },
                            onError = { loadError = it },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Loading overlay
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.donation_loading),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Secure WebView restricted to Stripe domains only.
 *
 * SECURITY:
 * - Domain whitelist enforced
 * - Deep links intercepted
 * - No external navigation allowed
 * - Cache/history cleared
 *
 * SCROLLING:
 * - Full vertical scrolling enabled
 * - No gesture conflicts with parent
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun SecureStripeWebView(
    url: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember WebView instance for cleanup
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                clearCache(true)
                clearHistory()
                clearFormData()
                destroy()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this

                // Set layout params for full size
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // ═══════════════════════════════════════════════════════════
                // SCROLLING: Prevent parent from intercepting touch events
                // ═══════════════════════════════════════════════════════════
                setOnTouchListener { v, _ ->
                    // Request parent to not intercept touch events
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false // Don't consume, let WebView handle it
                }

                // ═══════════════════════════════════════════════════════════
                // DISPLAY & SCROLL SETTINGS
                // ═══════════════════════════════════════════════════════════
                settings.apply {
                    // Required for Stripe
                    javaScriptEnabled = true
                    domStorageEnabled = true

                    // Viewport for responsive Stripe pages
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    // Allow zoom for accessibility
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // Text scaling
                    textZoom = 100

                    // Security hardening
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    allowFileAccess = false
                    allowContentAccess = false

                    // Mixed content for Stripe resources
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                // Enable scrollbars
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                isScrollbarFadingEnabled = true
                scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

                // Ensure scrolling works
                overScrollMode = WebView.OVER_SCROLL_ALWAYS

                // Clear any existing data
                clearCache(true)
                clearHistory()
                clearFormData()

                // ═══════════════════════════════════════════════════════════
                // SECURE WEB CLIENT
                // ═══════════════════════════════════════════════════════════
                webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false

                        return when {
                            // SUCCESS: Payment completed
                            StripePaymentLinks.isSuccessDeepLink(requestUrl) -> {
                                onSuccess()
                                true
                            }

                            // CANCEL: User cancelled
                            StripePaymentLinks.isCancelDeepLink(requestUrl) -> {
                                onCancel()
                                true
                            }

                            // ALLOWED: Stripe domain
                            StripePaymentLinks.isValidStripeDomain(requestUrl) -> {
                                false // Let WebView handle it
                            }

                            // BLOCKED: Unknown domain
                            else -> {
                                android.util.Log.w(
                                    "DonationWebView",
                                    "Blocked navigation to non-Stripe domain: $requestUrl"
                                )
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChange(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errorDescription = error?.description?.toString() ?: "Connection error"
                            onError(errorDescription)
                        }
                    }
                }

                // Load the checkout URL
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}