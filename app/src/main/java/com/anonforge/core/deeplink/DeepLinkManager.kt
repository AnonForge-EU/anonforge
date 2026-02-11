package com.anonforge.core.deeplink

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for handling deep link events across the app.
 *
 * Architecture:
 * 1. MainActivity receives deep link intent
 * 2. MainActivity calls handleDeepLink(uri)
 * 3. DeepLinkManager emits event to SharedFlow
 * 4. SupportViewModel collects events and updates UI
 *
 * Supported deep links:
 * - anonforge://donation/success (payment completed)
 * - anonforge://donation/cancel (payment cancelled)
 */
@Singleton
class DeepLinkManager @Inject constructor() {

    private val _donationEvents = MutableSharedFlow<DonationDeepLinkEvent>(
        extraBufferCapacity = 1,
        replay = 0
    )
    val donationEvents: SharedFlow<DonationDeepLinkEvent> = _donationEvents.asSharedFlow()

    /**
     * Process incoming deep link URI.
     * Call this from MainActivity when receiving deep links.
     *
     * @param uri The deep link URI (e.g., anonforge://donation/success)
     */
    fun handleDeepLink(uri: Uri) {
        // Security: Only process our custom scheme
        if (uri.scheme != SCHEME_ANONFORGE) return

        when (uri.host) {
            HOST_DONATION -> handleDonationDeepLink(uri)
            // Add other deep link hosts here as needed
        }
    }

    private fun handleDonationDeepLink(uri: Uri) {
        val event = when (uri.path) {
            PATH_SUCCESS -> DonationDeepLinkEvent.Success
            PATH_CANCEL -> DonationDeepLinkEvent.Cancelled
            else -> return
        }
        _donationEvents.tryEmit(event)
    }

    companion object {
        const val SCHEME_ANONFORGE = "anonforge"
        const val HOST_DONATION = "donation"
        const val PATH_SUCCESS = "/success"
        const val PATH_CANCEL = "/cancel"
    }
}

/**
 * Sealed class representing donation deep link events.
 */
sealed class DonationDeepLinkEvent {
    data object Success : DonationDeepLinkEvent()
    data object Cancelled : DonationDeepLinkEvent()
}