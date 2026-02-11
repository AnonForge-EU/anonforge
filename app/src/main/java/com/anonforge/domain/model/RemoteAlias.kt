package com.anonforge.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a remote alias fetched from external services.
 *
 * NOTE: Phone/Twilio support has been permanently removed from AnonForge.
 * Only Email aliases (SimpleLogin) are supported.
 */
sealed class RemoteAlias {
    abstract val id: String
    abstract val displayValue: String
    abstract val createdAt: Instant?
    abstract val isEnabled: Boolean

    /**
     * Email alias from SimpleLogin.
     *
     * @param id Unique identifier from SimpleLogin
     * @param email The email address
     * @param note User note for alias management
     * @param createdAt Creation timestamp
     * @param isEnabled Whether alias is active
     * @param forwardCount Number of forwarded emails
     * @param blockCount Number of blocked emails
     */
    data class Email(
        override val id: String,
        val email: String,
        val note: String?,
        override val createdAt: Instant?,
        override val isEnabled: Boolean,
        val forwardCount: Int = 0,
        val blockCount: Int = 0
    ) : RemoteAlias() {
        override val displayValue: String get() = email
        val domain: String get() = email.substringAfter("@", "")
        val prefix: String get() = email.substringBefore("@", "")
        val maskedDisplay: String
            get() = if (prefix.length > 4) {
                "${prefix.take(2)}...${prefix.takeLast(2)}@$domain"
            } else {
                email
            }
    }
}

/**
 * Result of fetching aliases from remote service.
 */
sealed class FetchAliasesResult {
    /**
     * Successfully fetched aliases.
     *
     * @param aliases List of fetched aliases
     * @param totalCount Total count on server for pagination
     * @param hasMore Whether more pages exist
     */
    data class Success(
        val aliases: List<RemoteAlias>,
        val totalCount: Int,
        val hasMore: Boolean = false
    ) : FetchAliasesResult()

    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val retryAfterSeconds: Int? = null
    ) : FetchAliasesResult()

    data object NotConfigured : FetchAliasesResult()
    data object Empty : FetchAliasesResult()
}

/**
 * Result of importing aliases to local storage.
 */
data class ImportResult(
    val successCount: Int,
    val failureCount: Int,
    val duplicateCount: Int,
    val errors: List<String> = emptyList()
) {
    @Suppress("unused") // Computed property for import statistics display
    val totalProcessed: Int get() = successCount + failureCount + duplicateCount
    val isFullSuccess: Boolean get() = failureCount == 0
}