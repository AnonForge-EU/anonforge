package com.anonforge.feature.splash

import com.anonforge.data.local.prefs.SecurityPreferences
import com.anonforge.data.repository.PreferencesRepository
import com.anonforge.data.repository.SecurityPreferencesRepository
import com.anonforge.security.encryption.KeyManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        preferencesRepository = mockk()
        securityPreferencesRepository = mockk()
        securityPreferences = mockk()
        keyManager = mockk()

        // Baseline: vault key present, disclaimer accepted, no auth → MAIN.
        // Each test overrides only what it needs.
        every { keyManager.isVaultKeyLost() } returns false
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
        securityPreferences = securityPreferences,
        keyManager = keyManager
    )

    /**
     * checkInitialState hops through Dispatchers.IO (vault-key check), which the
     * test scheduler does not control — advanceUntilIdle() would assert before
     * the IO hop completes. Instead, suspend on a real dispatcher until the
     * ViewModel publishes a target (the real-time wrapper is the documented way
     * to keep withTimeout from expiring in virtual time); the timeout only turns
     * a never-emitting regression into a clean failure.
     */
    @Suppress("OPT_IN_USAGE") // limitedParallelism is stable enough for tests
    private suspend fun SplashViewModel.awaitTarget(): SplashNavigationTarget =
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) {
                state.first { it.navigationTarget != null }.navigationTarget!!
            }
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // Nominal routing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `disclaimer not accepted routes to DISCLAIMER`() = runTest {
        coEvery { preferencesRepository.isDisclaimerAccepted() } returns false

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.DISCLAIMER, viewModel.awaitTarget())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `pin-only setup routes to UNLOCK`() = runTest {
        coEvery { securityPreferences.hasPin() } returns true

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }

    @Test
    fun `biometric-only setup routes to UNLOCK`() = runTest {
        every { securityPreferencesRepository.biometricEnabledFlow } returns flowOf(true)

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }

    @Test
    fun `no auth configured routes to MAIN`() = runTest {
        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.MAIN, viewModel.awaitTarget())
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

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
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

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }

    @Test
    fun `hasPin read failure routes to UNLOCK, never MAIN`() = runTest {
        coEvery { securityPreferences.hasPin() } throws
            IOException("security_prefs corrupted")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }

    @Test
    fun `unexpected runtime failure during checks also routes to UNLOCK`() = runTest {
        // Not just IOException: any unexpected error must stay fail-closed.
        coEvery { securityPreferences.hasPin() } throws IllegalStateException("boom")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lost vault key routing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `lost vault key routes to VAULT_UNREADABLE before anything else`() = runTest {
        every { keyManager.isVaultKeyLost() } returns true
        // Even with a PIN configured, the terminal error screen takes priority:
        // unlocking would only lead to opening an unreadable database.
        coEvery { securityPreferences.hasPin() } returns true

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(
            SplashNavigationTarget.VAULT_UNREADABLE,
            viewModel.awaitTarget()
        )
    }

    @Test
    fun `vault key check failure routes to UNLOCK, never MAIN`() = runTest {
        // isVaultKeyLost is designed not to throw, but if it ever does the
        // generic fail-closed path must keep the lock screen in front.
        every { keyManager.isVaultKeyLost() } throws IllegalStateException("keystore error")

        val viewModel = buildViewModel()
        viewModel.checkInitialState()

        assertEquals(SplashNavigationTarget.UNLOCK, viewModel.awaitTarget())
    }
}
