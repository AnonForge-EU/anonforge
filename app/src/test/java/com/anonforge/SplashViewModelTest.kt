package com.anonforge.feature.splash

import com.anonforge.data.local.prefs.SecurityPreferences
import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.data.repository.SecurityPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [SplashViewModel] launch routing.
 *
 * The fail-closed cases are the security-critical ones: when any preference
 * store is unreadable (DataStore CorruptionException/IOException), routing
 * must go to UNLOCK — an I/O error must never open the vault (MAIN) without
 * asking for the PIN.
 */
class SplashViewModelTest {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var securityPreferencesRepository: SecurityPreferencesRepository
    private lateinit var securityPreferences: SecurityPreferences

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        preferencesRepository = mockk()
        securityPreferencesRepository = mockk()
        securityPreferences = mockk()

        // Baseline: disclaimer accepted, no auth configured → MAIN.
        // Each test overrides only what it needs.
        coEvery { preferencesRepository.isDisclaimerAccepted() } returns true
        every { securityPreferencesRepository.biometricEnabledFlow } returns flowOf(false)
        coEvery { securityPreferences.hasPin() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SplashViewModel(
        preferencesRepository = preferencesRepository,
        securityPreferencesRepository = securityPreferencesRepository,
        securityPreferences = securityPreferences
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // Nominal routing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `disclaimer not accepted routes to DISCLAIMER`() = runTest {
        coEvery { preferencesRepository.isDisclaimerAccepted() } returns false

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.DISCLAIMER, viewModel.state.value.navigationTarget)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `pin-only setup routes to UNLOCK`() = runTest {
        coEvery { securityPreferences.hasPin() } returns true

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
    }

    @Test
    fun `biometric-only setup routes to UNLOCK`() = runTest {
        every { securityPreferencesRepository.biometricEnabledFlow } returns flowOf(true)

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
    }

    @Test
    fun `no auth configured routes to MAIN`() = runTest {
        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.MAIN, viewModel.state.value.navigationTarget)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Fail-closed routing on read errors
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `disclaimer read failure routes to UNLOCK, never MAIN`() = runTest {
        coEvery { preferencesRepository.isDisclaimerAccepted() } throws
            IOException("prefs unreadable")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `biometric flag read failure routes to UNLOCK, never MAIN`() = runTest {
        // DataStore surfaces a corrupted security_prefs file as an exception
        // thrown from the flow itself.
        every { securityPreferencesRepository.biometricEnabledFlow } returns
            flow { throw IOException("security_prefs corrupted") }

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
    }

    @Test
    fun `hasPin read failure routes to UNLOCK, never MAIN`() = runTest {
        coEvery { securityPreferences.hasPin() } throws
            IOException("security_prefs corrupted")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
    }

    @Test
    fun `unexpected runtime failure during checks also routes to UNLOCK`() = runTest {
        // Not just IOException: any unexpected error must stay fail-closed.
        coEvery { securityPreferences.hasPin() } throws IllegalStateException("boom")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()
        advanceUntilIdle()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.state.value.navigationTarget)
    }
}
