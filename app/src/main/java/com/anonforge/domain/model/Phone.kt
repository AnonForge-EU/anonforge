package com.anonforge.domain.model

@JvmInline
value class Phone(val value: String) {
    init {
        require(value.matches(Regex("^\\+[1-9]\\d{1,14}$"))) {
            "Invalid E.164 phone format"
        }
    }

    val formatted: String
        get() {
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
            val last4 = value.takeLast(4)
            return "${value.take(2)} ****$last4"
        }
}