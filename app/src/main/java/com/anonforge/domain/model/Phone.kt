package com.anonforge.domain.model

@JvmInline
value class Phone(val value: String) {
    init {
        // An empty value means "no phone" — used by manually-entered identities
        // that omit the number. Non-empty values accept E.164 (generated
        // numbers) OR the same format the saved-numbers screen validates
        // (`+` optional, 8–15 digits): saved aliases and manual entries may
        // be national-format, and constructing Phone from them must never
        // throw (selecting such an alias used to crash the generator).
        // The union keeps every previously-valid value valid.
        require(
            value.isEmpty() ||
                value.matches(E164_REGEX) ||
                value.matches(USER_ENTERED_REGEX)
        ) {
            "Invalid phone format"
        }
    }

    /** True when no phone number is set. */
    val isBlank: Boolean
        get() = value.isEmpty()

    val formatted: String
        get() {
            if (value.isEmpty()) return ""
            // User-entered national numbers (no "+") are shown as saved.
            if (!value.startsWith("+")) return value
            val digits = value.substring(1)
            return when {
                digits.startsWith("1") && digits.length == 11 -> {
                    "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
                }
                digits.startsWith("33") && digits.length == 11 -> {
                    // Use indexing operator for single character extraction (more idiomatic Kotlin)
                    "+33 ${digits[2]} ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9)}"
                }
                else -> value
            }
        }

    /**
     * Masked display version for privacy (shows only last 4 digits).
     * Example: "+3 ****1234"
     */
    @Suppress("unused") // Public API for privacy-focused UI display
    val displayMasked: String
        get() {
            if (value.isEmpty()) return ""
            val last4 = value.takeLast(4)
            return "${value.take(2)} ****$last4"
        }

    companion object {
        /** Strict E.164: "+" then 2–15 digits, first digit non-zero. */
        private val E164_REGEX = Regex("^\\+[1-9]\\d{1,14}$")

        /** User-entered numbers as accepted by the saved-numbers screen. */
        private val USER_ENTERED_REGEX = Regex("^\\+?[0-9]{8,15}$")
    }
}