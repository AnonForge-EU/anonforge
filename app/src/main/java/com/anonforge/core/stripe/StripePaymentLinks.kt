package com.anonforge.core.stripe

import androidx.core.net.toUri
import com.anonforge.domain.model.DonationTier

/**
 * Stripe Payment Links configuration.
 *
 * CURRENT MODE: PRODUCTION (Live payments)
 *
 * Deep links configured in Stripe Dashboard:
 *   success_url → anonforge://donation/success
 *   cancel_url  → anonforge://donation/cancel
 *
 * SECURITY:
 * - Payment Links are public URLs (no API key needed in app)
 * - All payment processing happens on Stripe's secure servers
 * - App never sees card data
 * - Deep links handle success/cancel callbacks
 */
object StripePaymentLinks {

    // ═══════════════════════════════════════════════════════════════════════════
    // DEEP LINK URLS (for Stripe redirect)
    // ═══════════════════════════════════════════════════════════════════════════

    const val SUCCESS_URL = "anonforge://donation/success"
    const val CANCEL_URL = "anonforge://donation/cancel"

    // ═══════════════════════════════════════════════════════════════════════════
    // ONE-TIME PAYMENT LINKS (PRODUCTION)
    // ═══════════════════════════════════════════════════════════════════════════

    // Coffee - 3€
    private const val LINK_COFFEE =
        "https://buy.stripe.com/00weVd84UdkI1jJ4acdby05"

    // Lunch - 10€
    private const val LINK_LUNCH =
        "https://buy.stripe.com/aFa8wPfxmbcAfaz8qsdby04"

    // Champion - 25€
    private const val LINK_CHAMPION =
        "https://buy.stripe.com/6oU28rad2bcA0fFfSUdby03"

    // ═══════════════════════════════════════════════════════════════════════════
    // MONTHLY SUBSCRIPTION LINKS (PRODUCTION)
    // ═══════════════════════════════════════════════════════════════════════════

    // Monthly Supporter - 3€/month
    private const val LINK_MONTHLY_SUPPORTER =
        "https://buy.stripe.com/9B6eVd4SI0xWaUj7modby02"

    // Monthly Pro - 5€/month
    private const val LINK_MONTHLY_PRO =
        "https://buy.stripe.com/00wfZh0Cs0xW6E3cGIdby01"

    // Monthly Hero - 10€/month
    private const val LINK_MONTHLY_HERO =
        "https://buy.stripe.com/28E5kD84U5Sg3rRgWYdby00"

    /**
     * Get the Payment Link URL for a given donation tier.
     *
     * @param tier The donation tier selected by user
     * @return The Stripe Payment Link URL to open in WebView
     */
    fun getPaymentLink(tier: DonationTier): String {
        return when (tier) {
            is DonationTier.Coffee -> LINK_COFFEE
            is DonationTier.Lunch -> LINK_LUNCH
            is DonationTier.Champion -> LINK_CHAMPION
            is DonationTier.MonthlySupporter -> LINK_MONTHLY_SUPPORTER
            is DonationTier.MonthlyPro -> LINK_MONTHLY_PRO
            is DonationTier.MonthlyHero -> LINK_MONTHLY_HERO
        }
    }

    /**
     * Validate that a URL is a valid Stripe domain.
     * Used for WebView security.
     */
    fun isValidStripeDomain(url: String): Boolean {
        val allowedDomains = listOf(
            "stripe.com",
            "checkout.stripe.com",
            "buy.stripe.com",
            "js.stripe.com",
            "m.stripe.com",
            "pay.stripe.com",
            "billing.stripe.com"
        )

        return try {
            val host = url.toUri().host?.lowercase() ?: return false
            allowedDomains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if URL is success deep link.
     */
    fun isSuccessDeepLink(url: String): Boolean {
        return url.startsWith(SUCCESS_URL)
    }

    /**
     * Check if URL is cancel deep link.
     */
    fun isCancelDeepLink(url: String): Boolean {
        return url.startsWith(CANCEL_URL)
    }
}