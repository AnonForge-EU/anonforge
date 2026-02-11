package com.anonforge.domain.model

/**
 * Supported nationalities for identity generation.
 * Determines name datasets, address formats, and phone prefixes.
 */
enum class Nationality(
    val code: String,
    @Suppress("unused") // String resource key for i18n display in nationality selector
    val displayNameKey: String,
    val phonePrefix: String
) {
    FR("fr", "nationality_fr", "+33"),
    EN("en", "nationality_en", "+44"),
    DE("de", "nationality_de", "+49");

    companion object {
        val DEFAULT = FR

        fun fromCode(code: String): Nationality {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}