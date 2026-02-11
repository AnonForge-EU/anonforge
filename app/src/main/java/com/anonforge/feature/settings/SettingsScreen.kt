package com.anonforge.feature.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.domain.model.AppLanguage
import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.ThemeMode
import com.anonforge.ui.components.BiometricSetupDialog
import com.anonforge.ui.components.PinSetupDialog
import com.anonforge.ui.components.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAliasSettings: (() -> Unit)? = null,
    onNavigateToPhoneAliasSettings: (() -> Unit)? = null,
    onNavigateToSupport: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context: Context = LocalContext.current

    val fingerprintSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBiometricStatus()
    }

    LaunchedEffect(state.launchFingerprintSettings) {
        if (state.launchFingerprintSettings) {
            val intent = viewModel.getFingerprintSettingsIntent()
            fingerprintSettingsLauncher.launch(intent)
            viewModel.clearFingerprintSettingsLaunch()
        }
    }

    LaunchedEffect(state.showBiometricVerificationPending) {
        if (state.showBiometricVerificationPending) {
            val activity = context.findActivity()
            if (activity != null) {
                showBiometricPrompt(
                    activity = activity,
                    title = context.getString(R.string.biometric_setup_title),
                    subtitle = context.getString(R.string.biometric_prompt_subtitle),
                    onSuccess = { viewModel.onBiometricVerificationSuccess() },
                    onError = { viewModel.onBiometricVerificationFailed() },
                    onCancel = { viewModel.onBiometricVerificationCancelled() }
                )
            } else {
                android.util.Log.e("SettingsScreen", "Could not find FragmentActivity for BiometricPrompt")
                viewModel.onBiometricVerificationCancelled()
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(state.languageChangeRequested) {
        if (state.languageChangeRequested) {
            val localeCode = when (state.appLanguage) {
                AppLanguage.SYSTEM -> ""
                AppLanguage.EN -> "en"
                AppLanguage.FR -> "fr"
            }

            val localeList = if (localeCode.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(localeCode)
            }

            AppCompatDelegate.setApplicationLocales(localeList)
            viewModel.clearLanguageChangeRequest()
        }
    }

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ═══════════════════════════════════════════════════════════════════
                // SECURITY SECTION
                // ═══════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.settings_security),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.biometricAvailable) {
                            SettingsRow(
                                icon = Icons.Default.Fingerprint,
                                title = stringResource(R.string.settings_biometric),
                                subtitle = when {
                                    state.biometricEnabled -> stringResource(R.string.settings_biometric_subtitle)
                                    state.biometricEnrolled -> stringResource(R.string.biometric_tap_to_enable)
                                    else -> stringResource(R.string.biometric_not_enrolled_title)
                                },
                                trailing = {
                                    Switch(
                                        checked = state.biometricEnabled,
                                        onCheckedChange = { viewModel.toggleBiometric(it) }
                                    )
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_pin_title),
                            subtitle = if (state.pinConfigured)
                                stringResource(R.string.settings_pin_configured)
                            else
                                stringResource(R.string.settings_pin_not_configured),
                            trailing = {
                                if (state.pinConfigured) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.showPinSetup() }) {
                                            Text(stringResource(R.string.settings_change_pin))
                                        }
                                        TextButton(onClick = { viewModel.removePin() }) {
                                            Text(
                                                stringResource(R.string.remove),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else {
                                    TextButton(onClick = { viewModel.showPinSetup() }) {
                                        Text(stringResource(R.string.settings_setup_pin))
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsRow(
                            icon = Icons.Default.Timer,
                            title = stringResource(R.string.settings_autolock),
                            subtitle = getAutoLockLabel(state.autoLockMinutes),
                            trailing = {
                                TextButton(onClick = { viewModel.showAutoLockDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ═══════════════════════════════════════════════════════════════════
                // APPEARANCE SECTION (Skill 18)
                // ═══════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Default.BrightnessMedium,
                            title = stringResource(R.string.settings_theme_title),
                            subtitle = getThemeModeLabel(state.themeMode),
                            trailing = {
                                TextButton(onClick = { viewModel.showThemeDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsRow(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.settings_language_title),
                            subtitle = getLanguageLabel(state.appLanguage),
                            trailing = {
                                TextButton(onClick = { viewModel.showLanguageDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ═══════════════════════════════════════════════════════════════════
                // GENERATION SECTION
                // ═══════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.settings_generation),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Flag,
                            title = stringResource(R.string.settings_nationality),
                            subtitle = getNationalityLabel(state.selectedNationality),
                            trailing = {
                                TextButton(onClick = { viewModel.showNationalityDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Gender Preference
                        SettingsRow(
                            icon = Icons.Default.Person,
                            title = stringResource(R.string.settings_gender),
                            subtitle = getGenderPreferenceLabel(state.selectedGenderPreference),
                            trailing = {
                                TextButton(onClick = { viewModel.showGenderDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsRow(
                            icon = Icons.Default.Cake,
                            title = stringResource(R.string.settings_age_range),
                            subtitle = "${state.ageRangeMin} - ${state.ageRangeMax} ${stringResource(R.string.years)}",
                            trailing = {
                                TextButton(onClick = { viewModel.showAgeRangeDialog() }) {
                                    Text(stringResource(R.string.change))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ═══════════════════════════════════════════════════════════════════
                // ALIAS SECTION (Email + Phone)
                // ═══════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.settings_email_alias),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onNavigateToAliasSettings != null) {
                                Modifier.clickable { onNavigateToAliasSettings() }
                            } else {
                                Modifier
                            }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_simplelogin),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (state.aliasConfigured)
                                        stringResource(R.string.settings_alias_configured)
                                    else
                                        stringResource(R.string.settings_alias_not_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (onNavigateToAliasSettings != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.configure),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onNavigateToPhoneAliasSettings != null) {
                                Modifier.clickable { onNavigateToPhoneAliasSettings() }
                            } else {
                                Modifier
                            }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.phone_alias_settings_title),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (state.phoneAliasConfigured)
                                        stringResource(R.string.phone_alias_key_configured)
                                    else
                                        stringResource(R.string.settings_alias_not_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (onNavigateToPhoneAliasSettings != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.configure),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ═══════════════════════════════════════════════════════════════════
                // ABOUT & SUPPORT SECTION
                // ═══════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (onNavigateToSupport != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSupport() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.support_title),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.support_header_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AnonForge",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.app_version, "1.0.0"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // DIALOGS
        // ═══════════════════════════════════════════════════════════════════════════

        if (state.showBiometricEnrollmentDialog) {
            BiometricSetupDialog(
                onDismiss = { viewModel.dismissBiometricEnrollmentDialog() },
                onSetupClick = { viewModel.onGoToFingerprintSettings() }
            )
        }

        if (state.showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { viewModel.dismissPinSetup() },
                onPinSet = { pin: CharArray ->
                    val pinString = String(pin)
                    pin.fill('0')
                    viewModel.setPin(pinString)
                }
            )
        }

        if (state.showAutoLockDialog) {
            AutoLockDialog(
                currentMinutes = state.autoLockMinutes,
                onSelect = { minutes ->
                    viewModel.setAutoLockMinutes(minutes)
                    viewModel.dismissAutoLockDialog()
                },
                onDismiss = { viewModel.dismissAutoLockDialog() }
            )
        }

        if (state.showNationalityDialog) {
            NationalityDialog(
                currentNationality = state.selectedNationality,
                onSelect = { nationality ->
                    viewModel.setNationality(nationality)
                    viewModel.dismissNationalityDialog()
                },
                onDismiss = { viewModel.dismissNationalityDialog() }
            )
        }

        if (state.showGenderDialog) {
            GenderPreferenceDialog(
                currentPreference = state.selectedGenderPreference,
                onSelect = { preference ->
                    viewModel.setGenderPreference(preference)
                },
                onDismiss = { viewModel.dismissGenderDialog() }
            )
        }

        if (state.showAgeRangeDialog) {
            AgeRangeDialog(
                currentMin = state.ageRangeMin,
                currentMax = state.ageRangeMax,
                onConfirm = { min, max ->
                    viewModel.setAgeRange(min, max)
                    viewModel.dismissAgeRangeDialog()
                },
                onDismiss = { viewModel.dismissAgeRangeDialog() }
            )
        }

        if (state.showThemeDialog) {
            ThemeModeDialog(
                currentMode = state.themeMode,
                onSelect = { mode -> viewModel.setThemeMode(mode) },
                onDismiss = { viewModel.dismissThemeDialog() }
            )
        }

        if (state.showLanguageDialog) {
            LanguageDialog(
                currentLanguage = state.appLanguage,
                onSelect = { language -> viewModel.setAppLanguage(language) },
                onDismiss = { viewModel.dismissLanguageDialog() }
            )
        }
    }
}

private fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel()
                } else {
                    onError()
                }
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(activity.getString(R.string.cancel))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

@Composable
private fun AutoLockDialog(
    currentMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to stringResource(R.string.settings_autolock_never),
        1 to stringResource(R.string.settings_autolock_1min),
        5 to stringResource(R.string.settings_autolock_5min),
        15 to stringResource(R.string.settings_autolock_15min)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_autolock)) },
        text = {
            Column {
                options.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMinutes == minutes,
                            onClick = { onSelect(minutes) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun NationalityDialog(
    currentNationality: Nationality,
    onSelect: (Nationality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_nationality)) },
        text = {
            Column {
                Nationality.entries.forEach { nationality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(nationality) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentNationality == nationality,
                            onClick = { onSelect(nationality) }
                        )
                        Text(
                            text = getNationalityLabel(nationality),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun GenderPreferenceDialog(
    currentPreference: GenderPreference,
    onSelect: (GenderPreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_gender)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderPreference.entries.forEach { preference ->
                    val isSelected = currentPreference == preference
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        label = "gender_bg"
                    )

                    val preferenceLabel = getGenderPreferenceLabel(preference)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = backgroundColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .clickable { onSelect(preference) }
                            .padding(12.dp)
                            .semantics { contentDescription = preferenceLabel },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (preference) {
                                GenderPreference.RANDOM -> Icons.Default.Shuffle
                                GenderPreference.MALE -> Icons.Default.Person
                                GenderPreference.FEMALE -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = preferenceLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeRangeDialog(
    currentMin: Int,
    currentMax: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember {
        mutableStateOf(currentMin.toFloat()..currentMax.toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_age_range)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "${sliderPosition.start.toInt()} - ${sliderPosition.endInclusive.toInt()} ${stringResource(R.string.years)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                RangeSlider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = GenerationPreferences.MIN_AGE.toFloat()..GenerationPreferences.MAX_AGE.toFloat(),
                    steps = GenerationPreferences.MAX_AGE - GenerationPreferences.MIN_AGE - 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${GenerationPreferences.MIN_AGE}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${GenerationPreferences.MAX_AGE}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(sliderPosition.start.toInt(), sliderPosition.endInclusive.toInt())
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        label = "theme_bg"
                    )

                    val modeLabel = getThemeModeLabel(mode)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .clickable { onSelect(mode) }
                            .padding(12.dp)
                            .semantics { contentDescription = modeLabel },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Default.Smartphone
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_language_restart_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onSelect(language) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = getLanguageLabel(language),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (language != AppLanguage.SYSTEM) {
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun getAutoLockLabel(minutes: Int): String {
    return when (minutes) {
        0 -> stringResource(R.string.settings_autolock_never)
        1 -> stringResource(R.string.settings_autolock_1min)
        5 -> stringResource(R.string.settings_autolock_5min)
        15 -> stringResource(R.string.settings_autolock_15min)
        else -> "$minutes min"
    }
}

@Composable
private fun getNationalityLabel(nationality: Nationality): String {
    return when (nationality) {
        Nationality.FR -> stringResource(R.string.nationality_fr)
        Nationality.EN -> stringResource(R.string.nationality_en)
        Nationality.DE -> stringResource(R.string.nationality_de)
    }
}

@Composable
private fun getGenderPreferenceLabel(preference: GenderPreference): String {
    return when (preference) {
        GenderPreference.RANDOM -> stringResource(R.string.gender_random)
        GenderPreference.MALE -> stringResource(R.string.gender_male)
        GenderPreference.FEMALE -> stringResource(R.string.gender_female)
    }
}

@Composable
private fun getThemeModeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
}

@Composable
private fun getLanguageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
        AppLanguage.EN -> stringResource(R.string.language_en)
        AppLanguage.FR -> stringResource(R.string.language_fr)
    }
}