package com.anonforge.domain.model

/**
 * Domain model for a simple email address.
 * Immutable value object wrapping email string.
 * 
 * Used in DomainIdentity to store the email address.
 * For alias-specific data (id, createdAt, etc.), see AliasEmail.
 */
@JvmInline
value class Email(val value: String) {
    
    /**
     * Extract domain part of email.
     */
    val domain: String
        get() = value.substringAfter("@", "")
    
    /**
     * Extract local part (before @) of email.
     */
    val localPart: String
        get() = value.substringBefore("@", "")
    
    /**
     * Basic validation - checks for @ and domain.
     */
    @Suppress("unused") // Public API for email validation
    val isValid: Boolean
        get() = value.contains("@") && domain.isNotEmpty() && localPart.isNotEmpty()
    
    /**
     * Masked display version for UI (shows partial email).
     * Example: "ab...cd@domain.com"
     */
    @Suppress("unused") // Public API for secure UI display
    val maskedDisplay: String
        get() {
            val local = localPart
            return if (local.length > 4) {
                "${local.take(2)}...${local.takeLast(2)}@$domain"
            } else {
                value
            }
        }
    
    override fun toString(): String = value
}
