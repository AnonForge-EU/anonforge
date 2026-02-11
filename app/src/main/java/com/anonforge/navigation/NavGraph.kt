package com.anonforge.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anonforge.feature.aliasimport.AliasImportDialog
import com.anonforge.feature.aliasimport.AliasImportViewModel
import com.anonforge.feature.disclaimer.DisclaimerScreen
import com.anonforge.feature.generator.GeneratorScreen
import com.anonforge.feature.phonealias.PhoneAliasSettingsScreen
import com.anonforge.feature.settings.AliasHistoryScreen
import com.anonforge.feature.settings.SettingsScreen
import com.anonforge.feature.settings.alias.AliasSettingsScreen
import com.anonforge.feature.splash.SplashScreen
import com.anonforge.feature.support.SupportScreen
import com.anonforge.feature.support.WhyAnonForgePage
import com.anonforge.feature.unlock.UnlockScreen
import com.anonforge.feature.vault.VaultScreen

/**
 * Navigation routes for AnonForge app.
 * All route constants are defined here for type-safe navigation.
 */
object Routes {
    const val SPLASH = "splash"
    const val DISCLAIMER = "disclaimer"
    const val UNLOCK = "unlock"
    const val VAULT = "vault"
    const val GENERATOR = "generator"
    const val SETTINGS = "settings"
    const val ALIAS_SETTINGS = "alias_settings"
    const val ALIAS_HISTORY = "alias_history"
    const val ALIAS_IMPORT = "alias_import"
    const val PHONE_ALIAS_SETTINGS = "phone_alias_settings"

    // Skill 17: Support & Why
    const val SUPPORT = "support"
    const val WHY_ANONFORGE = "why_anonforge"
}

/**
 * Main navigation graph for AnonForge.
 *
 * Navigation Flow:
 * ```
 * SPLASH -> check state
 *     |-- Disclaimer not accepted -> DISCLAIMER -> VAULT
 *     |-- Biometric/PIN enabled -> UNLOCK -> VAULT
 *     +-- No auth required -> VAULT
 *
 * VAULT (main screen)
 *     |-- + button -> GENERATOR
 *     +-- Settings icon -> SETTINGS
 *
 * SETTINGS
 *     |-- Email Aliases -> ALIAS_SETTINGS
 *     |       |-- History -> ALIAS_HISTORY
 *     |       +-- Import -> ALIAS_IMPORT
 *     |-- Phone Aliases -> PHONE_ALIAS_SETTINGS (manual management)
 *     +-- Support & Why -> SUPPORT
 *             +-- Why AnonForge? -> WHY_ANONFORGE
 * ```
 *
 * Security notes:
 * - UNLOCK is only shown when biometric/PIN is configured
 * - popUpTo with inclusive=true prevents back navigation to auth screens
 * - Each auth screen manages FLAG_SECURE independently
 *
 * Skill 17.2 notes:
 * - Phone aliases are managed manually (Hushed/OnOff)
 * - Email import only via SimpleLogin API
 */
@Composable
fun AnonForgeNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ==================== Entry Flow ====================

        /**
         * Splash Screen - Entry point that determines initial navigation.
         * Checks: disclaimer accepted, auth required, etc.
         */
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToUnlock = {
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToDisclaimer = {
                    navController.navigate(Routes.DISCLAIMER) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        /**
         * Disclaimer Screen - First launch only.
         * User must accept before accessing the app.
         */
        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(
                onAccept = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.DISCLAIMER) { inclusive = true }
                    }
                }
            )
        }

        /**
         * Unlock Screen - Authentication gate.
         * Shows biometric prompt and/or PIN entry.
         * Only shown when biometric/PIN is enabled.
         */
        composable(Routes.UNLOCK) {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        // ==================== Main Screens ====================

        /**
         * Vault Screen - Main screen with identity list.
         * Central hub for viewing, editing, and managing identities.
         */
        composable(Routes.VAULT) {
            VaultScreen(
                onNavigateToGenerator = {
                    navController.navigate(Routes.GENERATOR)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        /**
         * Generator Screen - Create new identity.
         * Supports full and partial identity generation.
         */
        composable(Routes.GENERATOR) {
            GeneratorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAliasSettings = {
                    navController.navigate(Routes.ALIAS_SETTINGS)
                }
            )
        }

        // ==================== Settings Screens ====================

        /**
         * Settings Screen - App configuration.
         * Security, data management, and external integrations.
         */
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAliasSettings = {
                    navController.navigate(Routes.ALIAS_SETTINGS)
                },
                onNavigateToPhoneAliasSettings = {
                    navController.navigate(Routes.PHONE_ALIAS_SETTINGS)
                },
                onNavigateToSupport = {
                    navController.navigate(Routes.SUPPORT)
                }
            )
        }

        /**
         * Email Alias Settings - SimpleLogin integration.
         * Configure API key and email alias generation.
         */
        composable(Routes.ALIAS_SETTINGS) {
            AliasSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.ALIAS_HISTORY)
                },
                onNavigateToImport = {
                    navController.navigate(Routes.ALIAS_IMPORT)
                }
            )
        }

        /**
         * Alias History Screen - View and manage saved email aliases.
         * Allows deletion of individual aliases or clearing all history.
         */
        composable(Routes.ALIAS_HISTORY) {
            AliasHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Alias Import Screen - Import existing aliases from SimpleLogin.
         * Fetches remote email aliases and allows batch import to local history.
         *
         * NOTE: Skill 17.2 - Phone aliases are managed manually (Hushed/OnOff),
         * so this dialog only handles email import.
         */
        composable(Routes.ALIAS_IMPORT) {
            val viewModel: AliasImportViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            AliasImportDialog(
                onDismiss = { navController.popBackStack() },
                emailResult = state.emailFetchResult,
                isLoading = state.isLoading,
                importResult = state.importResult,
                onFetchEmails = { viewModel.fetchEmailAliases() },
                onImport = { emails -> viewModel.importSelected(emails) },
                onRetry = { viewModel.retry() }
            )
        }

        /**
         * Phone Alias Settings - Manual phone number management.
         * Users add phone numbers from external services (Hushed, OnOff, etc.)
         *
         * NOTE: Skill 17.2 - No Twilio API integration. Phone aliases are
         * manually managed by the user.
         */
        composable(Routes.PHONE_ALIAS_SETTINGS) {
            PhoneAliasSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ==================== Support Screens (Skill 17) ====================

        /**
         * Support Screen - Donations, affiliations, and educational content.
         * Provides ways to support AnonForge development.
         */
        composable(Routes.SUPPORT) {
            SupportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToWhyPage = {
                    navController.navigate(Routes.WHY_ANONFORGE)
                }
            )
        }

        /**
         * Why AnonForge Page - Educational content.
         * Explains data breaches, service trade-offs, and privacy policy.
         */
        composable(Routes.WHY_ANONFORGE) {
            WhyAnonForgePage(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}