package com.anonforge.domain.usecase

import com.anonforge.core.stripe.StripePaymentLinks
import com.anonforge.domain.model.CheckoutSession
import com.anonforge.domain.model.DonationTier
import javax.inject.Inject

/**
 * Use case for creating a Stripe checkout session.
 *
 * IMPLEMENTATION:
 * This use case uses Stripe Payment Links instead of the Checkout Sessions API.
 * Payment Links are pre-configured URLs that don't require a backend server.
 *
 * BENEFITS OF PAYMENT LINKS:
 * - No backend required
 * - No API keys in the app
 * - Secure by default
 * - Easy to configure in Stripe Dashboard
 * - Supports deep link redirects
 *
 * FLOW:
 * 1. User selects a donation tier
 * 2. This use case returns the appropriate Payment Link URL
 * 3. App opens URL in secure WebView
 * 4. User completes payment on Stripe's page
 * 5. Stripe redirects to anonforge://donation/success or /cancel
 * 6. App intercepts deep link and shows result
 */
class CreateStripeCheckoutUseCase @Inject constructor() {

    /**
     * Get the checkout URL for a donation tier.
     *
     * @param tier The donation tier selected by user
     * @return Result containing CheckoutSession with URL, or failure
     */
    operator fun invoke(tier: DonationTier): Result<CheckoutSession> {
        return try {
            // Get Payment Link URL
            val paymentLinkUrl = StripePaymentLinks.getPaymentLink(tier)

            Result.success(CheckoutSession(checkoutUrl = paymentLinkUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}