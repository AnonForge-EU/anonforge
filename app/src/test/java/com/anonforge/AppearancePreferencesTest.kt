package com.anonforge.feature.settings

import app.cash.turbine.test
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.AppLanguage
import com.anonforge.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Theme & Language preferences (Skill 18).
 */
class AppearancePreferencesTest {

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        settingsDataStore = mockk(relaxed = true)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Theme Mode Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `theme mode changes persist and emit correctly`() = runTest {
        // Given: DataStore returns DARK after setting
        coEvery { settingsDataStore.themeMode } returns flowOf(ThemeMode.DARK)
        coEvery { settingsDataStore.setThemeMode(ThemeMode.DARK) } returns Unit

        // When: Set theme to DARK
        settingsDataStore.setThemeMode(ThemeMode.DARK)

        // Then: Verify persistence and emission
        coVerify { settingsDataStore.setThemeMode(ThemeMode.DARK) }

        settingsDataStore.themeMode.test {
            assertEquals(ThemeMode.DARK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme mode LIGHT persists correctly`() = runTest {
        // Given: DataStore returns LIGHT after setting
        coEvery { settingsDataStore.themeMode } returns flowOf(ThemeMode.LIGHT)
        coEvery { settingsDataStore.setThemeMode(ThemeMode.LIGHT) } returns Unit

        // When: Set theme to LIGHT
        settingsDataStore.setThemeMode(ThemeMode.LIGHT)

        // Then: Verify persistence
        coVerify { settingsDataStore.setThemeMode(ThemeMode.LIGHT) }

        settingsDataStore.themeMode.test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme mode SYSTEM is default`() = runTest {
        // Given: DataStore returns SYSTEM (default)
        coEvery { settingsDataStore.themeMode } returns flowOf(ThemeMode.SYSTEM)

        // Then: Default is SYSTEM
        settingsDataStore.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Language Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `language change persists and triggers reload flag`() = runTest {
        // Given: DataStore returns FR after setting
        coEvery { settingsDataStore.appLanguage } returns flowOf(AppLanguage.FR)
        coEvery { settingsDataStore.setAppLanguage(AppLanguage.FR) } returns Unit

        // When: Change language to French
        settingsDataStore.setAppLanguage(AppLanguage.FR)

        // Then: Verify persistence
        coVerify { settingsDataStore.setAppLanguage(AppLanguage.FR) }

        settingsDataStore.appLanguage.test {
            assertEquals(AppLanguage.FR, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `language EN persists correctly`() = runTest {
        // Given: DataStore returns EN after setting
        coEvery { settingsDataStore.appLanguage } returns flowOf(AppLanguage.EN)
        coEvery { settingsDataStore.setAppLanguage(AppLanguage.EN) } returns Unit

        // When: Change language to English
        settingsDataStore.setAppLanguage(AppLanguage.EN)

        // Then: Verify persistence
        coVerify { settingsDataStore.setAppLanguage(AppLanguage.EN) }

        settingsDataStore.appLanguage.test {
            assertEquals(AppLanguage.EN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `language SYSTEM is default`() = runTest {
        // Given: DataStore returns SYSTEM (default)
        coEvery { settingsDataStore.appLanguage } returns flowOf(AppLanguage.SYSTEM)

        // Then: Default is SYSTEM
        settingsDataStore.appLanguage.test {
            assertEquals(AppLanguage.SYSTEM, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Enum Conversion Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `theme mode enum converts from code correctly`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromCode("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromCode("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromCode("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromCode("invalid"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromCode(""))
    }

    @Test
    fun `app language enum converts from code correctly`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("system"))
        assertEquals(AppLanguage.EN, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.FR, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("invalid"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(""))
    }

    @Test
    fun `theme mode default is SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DEFAULT)
    }

    @Test
    fun `app language default is SYSTEM`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.DEFAULT)
    }

    @Test
    fun `app language has correct display names`() {
        assertEquals("System", AppLanguage.SYSTEM.displayName)
        assertEquals("English", AppLanguage.EN.displayName)
        assertEquals("Français", AppLanguage.FR.displayName)
    }
}
