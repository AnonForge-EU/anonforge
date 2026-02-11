package com.anonforge.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

enum class ExpiryDuration(
    val duration: Duration,
    @Suppress("unused") // String resource key for i18n display in expiry selector
    val labelKey: String
) {
    ONE_DAY(1.days, "expiry_1d"),
    ONE_WEEK(7.days, "expiry_1w"),
    ONE_MONTH(30.days, "expiry_1m"),
    PERMANENT(Duration.INFINITE, "expiry_permanent");

    companion object {
        fun fromDays(days: Int): Duration = days.days
    }
}