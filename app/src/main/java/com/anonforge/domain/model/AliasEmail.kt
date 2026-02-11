package com.anonforge.domain.model

/**
 * Domain model for email alias.
 * Immutable value object representing an alias email address.
 */
data class AliasEmail(
    val id: Int,
    val email: String,
    val createdAt: Long,
    val isEnabled: Boolean
) {
    val domain: String
        get() = email.substringAfter("@", "")

    val prefix: String
        get() = email.substringBefore("@", "")

    /**
     * Masked display version for UI (shows partial email for privacy).
     * Example: "ab...xy@domain.com"
     */
    @Suppress("unused") // Public API for privacy-focused UI display in alias history
    val maskedDisplay: String
        get() {
            val prefix = this.prefix
            return if (prefix.length > 4) {
                "${prefix.take(2)}...${prefix.takeLast(2)}@$domain"
            } else {
                email
            }
        }
}