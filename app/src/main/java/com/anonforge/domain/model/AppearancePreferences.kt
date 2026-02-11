package com.anonforge.domain.model

/**
 * Theme mode preference.
 * SYSTEM follows device setting, LIGHT/DARK force specific theme.
 *
 * Skill 18: Theme & Language Selection
 */
enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = SYSTEM

        fun fromCode(code: String): ThemeMode {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}

/**
 * App language preference.
 * SYSTEM follows device locale, EN/FR force specific language.
 *
 * Skill 18: Theme & Language Selection
 */
enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "System"),
    EN("en", "English"),
    FR("fr", "Français");

    companion object {
        val DEFAULT = SYSTEM

        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}
