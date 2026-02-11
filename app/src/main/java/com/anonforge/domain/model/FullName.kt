package com.anonforge.domain.model

data class FullName(
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val gender: Gender
) {
    val fullDisplay: String
        get() = listOfNotNull(firstName, middleName, lastName).joinToString(" ")

    /**
     * Initials for compact UI display (e.g., avatar).
     * Example: "John Doe" → "JD"
     */
    @Suppress("unused") // Public API for compact UI display in identity cards
    val initialsDisplay: String
        get() = "${firstName.first()}${lastName.first()}".uppercase()
}