package com.anonforge.feature.support

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.core.deeplink.DeepLinkManager
import com.anonforge.core.deeplink.DonationDeepLinkEvent
import com.anonforge.domain.model.DonationResult
import com.anonforge.domain.model.DonationTier
import com.anonforge.domain.repository.DonationRepository
import com.anonforge.domain.usecase.CreateStripeCheckoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Support screen.
 *
 * PRIVACY: No personal data stored in state.
 * All payment handling happens on Stripe's servers.
 */
data class SupportState(
    // Donation configuration
    val isRecurringDonation: Boolean = false,
    val selectedTierId: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val checkoutUrl: String? = null,
    val showCheckoutWebView: Boolean = false,

    // Result feedback
    val donationResult: DonationResult? = null,

    // Supporter status
    val isSupporter: Boolean = false
) {
    /**
     * Check if donation button should be enabled.
     */
    val canDonate: Boolean
        get() = selectedTierId != null && !isLoading
}

/**
 * ViewModel for Support screen.
 *
 * Handles:
 * - Donation tier selection
 * - Stripe checkout flow
 * - Deep link callbacks
 * - Supporter status
 *
 * SECURITY:
 * - No payment data handled by app
 * - All checkout happens in secure WebView
 * - Deep links validated before processing
 *
 * ARCHITECTURE:
 * - Uses CreateStripeCheckoutUseCase for Payment Links
 * - Observes DeepLinkManager for success/cancel callbacks
 * - Persists supporter status via DonationRepository
 */
@HiltViewModel
class SupportViewModel @Inject constructor(
    private val createCheckoutUseCase: CreateStripeCheckoutUseCase,
    private val donationRepository: DonationRepository,
    private val deepLinkManager: DeepLinkManager
) : ViewModel() {

    private val _state = MutableStateFlow(SupportState())
    val state: StateFlow<SupportState> = _state.asStateFlow()

    init {
        checkSupporterStatus()
        observeDeepLinks()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private fun checkSupporterStatus() {
        viewModelScope.launch {
            val isSupporter = donationRepository.isSupporter()
            _state.update { it.copy(isSupporter = isSupporter) }
        }
    }

    private fun observeDeepLinks() {
        viewModelScope.launch {
            deepLinkManager.donationEvents.collect { event ->
                when (event) {
                    is DonationDeepLinkEvent.Success -> onDonationSuccess()
                    is DonationDeepLinkEvent.Cancelled -> onDonationCancelled()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Toggle between one-time and monthly donation.
     */
    fun toggleRecurring(isRecurring: Boolean) {
        _state.update { state ->
            state.copy(
                isRecurringDonation = isRecurring,
                // Reset selection when switching mode
                selectedTierId = null
            )
        }
    }

    /**
     * Select a donation tier.
     */
    fun selectTier(tierId: String) {
        _state.update { it.copy(selectedTierId = tierId) }
    }

    /**
     * Start the donation checkout flow.
     *
     * @param context Android context for external browser fallback
     * @param useExternalBrowser If true, open in external browser instead of WebView
     */
    fun startDonation(context: Context, useExternalBrowser: Boolean = false) {
        val currentState = _state.value
        val tier = getTierFromSelection(currentState) ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            createCheckoutUseCase(tier)
                .onSuccess { session ->
                    if (useExternalBrowser) {
                        // Open in external browser
                        val intent = Intent(Intent.ACTION_VIEW, session.checkoutUrl.toUri())
                        context.startActivity(intent)
                        _state.update { it.copy(isLoading = false) }
                    } else {
                        // Open in secure WebView
                        _state.update { state ->
                            state.copy(
                                isLoading = false,
                                checkoutUrl = session.checkoutUrl,
                                showCheckoutWebView = true
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            donationResult = DonationResult.Error(
                                error.message ?: "Unknown error"
                            )
                        )
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHECKOUT CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Dismiss the checkout WebView.
     */
    fun dismissCheckout() {
        _state.update { state ->
            state.copy(
                showCheckoutWebView = false,
                checkoutUrl = null
            )
        }
    }

    /**
     * Handle successful donation.
     * Called from WebView deep link interception or DeepLinkManager.
     */
    fun onDonationSuccess() {
        viewModelScope.launch {
            // Persist supporter status
            donationRepository.markAsSupporter()

            _state.update { state ->
                state.copy(
                    donationResult = DonationResult.Success,
                    showCheckoutWebView = false,
                    checkoutUrl = null,
                    selectedTierId = null,
                    isSupporter = true
                )
            }
        }
    }

    /**
     * Handle cancelled donation.
     */
    fun onDonationCancelled() {
        _state.update { state ->
            state.copy(
                donationResult = DonationResult.Cancelled,
                showCheckoutWebView = false,
                checkoutUrl = null
            )
        }
    }

    /**
     * Clear donation result after showing feedback.
     */
    fun clearDonationResult() {
        _state.update { it.copy(donationResult = null) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Convert UI selection to DonationTier.
     */
    private fun getTierFromSelection(state: SupportState): DonationTier? {
        return when (state.selectedTierId) {
            // One-time tiers
            "coffee" -> DonationTier.Coffee
            "lunch" -> DonationTier.Lunch
            "champion" -> DonationTier.Champion

            // Monthly tiers
            "monthly_supporter" -> DonationTier.MonthlySupporter
            "monthly_pro" -> DonationTier.MonthlyPro
            "monthly_hero" -> DonationTier.MonthlyHero

            else -> null
        }
    }
}