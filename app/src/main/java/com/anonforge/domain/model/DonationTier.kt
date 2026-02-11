package com.anonforge.domain.model

/**
 * Represents a donation tier with amount and recurrence.
 *
 * Predefined tiers:
 * - One-time: Coffee (3€), Lunch (10€), Champion (25€)
 * - Monthly: Supporter (3€/mo), Pro (5€/mo), Hero (10€/mo)
 *
 * All amounts are in cents for precision.
 */
sealed class DonationTier {
    abstract val id: String
    abstract val amountCents: Int
    abstract val isRecurring: Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // ONE-TIME TIERS
    // ═══════════════════════════════════════════════════════════════════════════

    data object Coffee : DonationTier() {
        override val id = "coffee"
        override val amountCents = 300  // 3€
        override val isRecurring = false
    }

    data object Lunch : DonationTier() {
        override val id = "lunch"
        override val amountCents = 1000  // 10€
        override val isRecurring = false
    }

    data object Champion : DonationTier() {
        override val id = "champion"
        override val amountCents = 2500  // 25€
        override val isRecurring = false
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MONTHLY TIERS
    // ═══════════════════════════════════════════════════════════════════════════

    data object MonthlySupporter : DonationTier() {
        override val id = "monthly_supporter"
        override val amountCents = 300  // 3€/month
        override val isRecurring = true
    }

    data object MonthlyPro : DonationTier() {
        override val id = "monthly_pro"
        override val amountCents = 500  // 5€/month
        override val isRecurring = true
    }

    data object MonthlyHero : DonationTier() {
        override val id = "monthly_hero"
        override val amountCents = 1000  // 10€/month
        override val isRecurring = true
    }

    companion object {
        /**
         * Get all one-time tiers for display.
         */
        fun getOneTimeTiers(): List<DonationTier> = listOf(Coffee, Lunch, Champion)

        /**
         * Get all monthly tiers for display.
         */
        fun getMonthlyTiers(): List<DonationTier> = listOf(MonthlySupporter, MonthlyPro, MonthlyHero)

        /**
         * Find tier by ID for payment callback handling.
         */
        @Suppress("unused") // Public API for Stripe webhook/callback tier resolution
        fun fromId(id: String): DonationTier? = when (id) {
            "coffee" -> Coffee
            "lunch" -> Lunch
            "champion" -> Champion
            "monthly_supporter" -> MonthlySupporter
            "monthly_pro" -> MonthlyPro
            "monthly_hero" -> MonthlyHero
            else -> null
        }
    }
}