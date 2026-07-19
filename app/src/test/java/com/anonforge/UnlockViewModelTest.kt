package com.anonforge.feature.unlock

import com.anonforge.security.auth.AuthManager
import com.anonforge.security.auth.AuthResult
import com.anonforge.security.auth.AuthState
import com.anonforge.security.auth.LockManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [UnlockViewModel] fail-closed behavior when the security
 * store is unreadable: the vault must stay locked with an explicit error —
 * no crash (SplashViewModel routes here on the same failure), no silent
 * unlock.
 */
class UnlockViewModelTest {

    private lateinit var authManager: AuthManager
    private lateinit var lockManager: LockManager

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        authManager = mockk(relaxed = true)
        lockManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `security store read failure keeps vault locked with explicit error`() = runTest {
        coEvery { authManager.isAuthConfigured() } throws
            IOException("security_prefs corrupted")

        val viewModel = UnlockViewModel(authManager, lockManager)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthState.RequiresAuth, state.authState)
        // PIN entry stays available as the recovery path; biometric is not
        // auto-triggered, so a persistently broken store cannot loop.
        assertTrue(state.pinAvailable)
        assertFalse(state.biometricAvailable)
        assertFalse(state.shouldTryBiometric)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `pin verification error surfaces message and stays locked`() = runTest {
        // A persistently unreadable store makes AuthManager.verifyPin() return
        // AuthResult.Error on every attempt — the dialog must show the error,
        // not unlock and not crash.
        coEvery { authManager.isAuthConfigured() } returns true
        every { lockManager.shouldRequireAuth() } returns true
        every { authManager.isLockedOut() } returns false
        coEvery { authManager.isBiometricEnabled() } returns false
        every { authManager.isBiometricEnrolled() } returns false
        coEvery { authManager.isPinConfigured() } returns true
        every { authManager.getRemainingAttempts() } returns 5
        coEvery { authManager.verifyPin(any()) } returns AuthResult.Error("Verification failed")

        val viewModel = UnlockViewModel(authManager, lockManager)
        advanceUntilIdle()

        viewModel.verifyPin(charArrayOf('1', '2', '3', '4', '5', '6'))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.authState is AuthState.Authenticated)
        assertEquals("Verification failed", state.pinError)
        assertFalse(state.isVerifyingPin)
    }
}
