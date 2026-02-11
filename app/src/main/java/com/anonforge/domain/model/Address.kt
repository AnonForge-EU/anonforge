@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.anonforge.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a physical address for a generated identity.
 *
 * Used in identity generation and export/import operations.
 * Serializable for JSON persistence and encrypted backup.
 */
@Serializable
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
) {
    /**
     * Full formatted address for display.
     * Example: "123 Main St, Paris 75001, France"
     */
    val displayFull: String
        get() = "$street, $city $zipCode, $country"
}