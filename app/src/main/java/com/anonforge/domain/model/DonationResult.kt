package com.anonforge.domain.model

/**
 * Represents the result of a donation attempt.
 * Used to display appropriate feedback to the user.
 */
sealed class DonationResult {
    /**
     * Donation completed successfully.
     * User should see thank you message and supporter badge.
     */
    data object Success : DonationResult()

    /**
     * User cancelled the donation.
     * No message shown, just dismiss checkout.
     */
    data object Cancelled : DonationResult()

    /**
     * Donation failed with an error.
     * Show error message to user.
     */
    data class Error(val message: String) : DonationResult()
}

/**
 * Represents a Stripe checkout session.
 * Contains the URL to redirect user for payment.
 */
data class CheckoutSession(
    val checkoutUrl: String
)