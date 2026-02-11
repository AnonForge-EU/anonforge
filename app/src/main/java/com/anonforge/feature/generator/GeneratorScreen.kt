package com.anonforge.feature.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.domain.model.ExpiryDuration
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import com.anonforge.feature.generator.components.AliasSelectionDialog
import com.anonforge.feature.generator.components.AliasSource
import com.anonforge.feature.generator.components.SelectableAlias
import com.anonforge.ui.components.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAliasSettings: (() -> Unit)? = null,
    onNavigateToPhoneAliasSettings: (() -> Unit)? = null,
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Refresh configurations when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshServiceConfigurations()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Collect events from ViewModel (ShowSnackbar, CopyToClipboard)
    // ═══════════════════════════════════════════════════════════════════════════
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GeneratorEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                    // Handle snackbar action (e.g., Copy button on alias created)
                    if (result == SnackbarResult.ActionPerformed && event.actionLabel != null) {
                        // Extract email from message "Alias created: xxx@yyy.com"
                        val email = event.message.substringAfter(": ", "")
                        if (email.isNotEmpty() && email.contains("@")) {
                            copyToClipboard(context, email)
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.copied_to_clipboard)
                            )
                        }
                    }
                }
                is GeneratorEvent.CopyToClipboard -> {
                    copyToClipboard(context, event.text)
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.copied_to_clipboard)
                    )
                }
            }
        }
    }

    // Handle save success - using i18n string
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.identity_saved_success)
            )
            viewModel.clearSavedState()
            onNavigateBack()
        }
    }

    // Handle errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALIAS SELECTION DIALOG
    // Shows when user clicks on email field to choose from imported aliases
    // ═══════════════════════════════════════════════════════════════════════════
    if (state.showAliasDialog) {
        // Convert AliasEmail to SelectableAlias for the dialog
        val selectableAliases = remember(state.existingAliases) {
            state.existingAliases.map { alias ->
                SelectableAlias(
                    id = alias.id.toString(),
                    value = alias.email,
                    displayName = null, // Could add note field if available
                    source = AliasSource.IMPORTED,
                    useCount = 0, // Could track usage count in future
                    isEnabled = alias.isEnabled
                )
            }
        }

        AliasSelectionDialog(
            title = stringResource(R.string.alias_select_title),
            aliases = selectableAliases,
            isLoading = state.isCreatingAlias,
            onSelect = { selectedAlias ->
                // Find the original AliasEmail and select it
                state.existingAliases.find { it.email == selectedAlias.value }?.let { alias ->
                    viewModel.selectAlias(alias)
                }
            },
            onCreateNew = {
                viewModel.createNewAlias()
            },
            onDismiss = {
                viewModel.hideAliasSelectionDialog()
            }
        )
    }

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.generator_title)) }
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
                // ═══════════════════════════════════════════════════════════════
                // GENDER SELECTION
                // ═══════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.generator_gender),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedGender == Gender.MALE,
                        onClick = { viewModel.setGender(Gender.MALE) },
                        label = { Text(stringResource(R.string.gender_male)) }
                    )
                    FilterChip(
                        selected = state.selectedGender == Gender.FEMALE,
                        onClick = { viewModel.setGender(Gender.FEMALE) },
                        label = { Text(stringResource(R.string.gender_female)) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ═══════════════════════════════════════════════════════════════
                // NATIONALITY SELECTION
                // ═══════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.generator_nationality),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Nationality.entries.forEach { nationality ->
                        FilterChip(
                            selected = state.selectedNationality == nationality,
                            onClick = { viewModel.setNationality(nationality) },
                            label = { Text(getNationalityLabel(nationality)) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ═══════════════════════════════════════════════════════════════
                // AGE RANGE
                // ═══════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.generator_age_range),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${state.ageRangeMin} - ${state.ageRangeMax} ${stringResource(R.string.years)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ═══════════════════════════════════════════════════════════════
                // INCLUDE ADDRESS TOGGLE
                // ═══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.generator_include_address))
                    Switch(
                        checked = state.includeAddress,
                        onCheckedChange = { viewModel.setIncludeAddress(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ═══════════════════════════════════════════════════════════════
                // TEMPORARY IDENTITY TOGGLE
                // ═══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.generator_temporary))
                        Text(
                            text = stringResource(R.string.expiry_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isTemporary,
                        onCheckedChange = { viewModel.setTemporary(it) }
                    )
                }

                // Expiry options (shown when temporary is enabled)
                AnimatedVisibility(
                    visible = state.isTemporary,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Expiry duration chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExpiryDuration.entries.filter { it != ExpiryDuration.PERMANENT }.forEach { expiry ->
                                FilterChip(
                                    selected = state.selectedExpiry == expiry && state.customDays == 0,
                                    onClick = { viewModel.setExpiryOption(expiry) },
                                    label = { Text(getExpiryLabel(expiry)) }
                                )
                            }
                        }

                        // Custom days input
                        OutlinedTextField(
                            value = if (state.customDays > 0) state.customDays.toString() else "",
                            onValueChange = { value ->
                                val days = value.toIntOrNull() ?: 0
                                viewModel.setCustomDays(days)
                            },
                            label = { Text(stringResource(R.string.generator_custom_days)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ═══════════════════════════════════════════════════════════════
                // IDENTITY PREVIEW
                // ═══════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.generator_preview),
                    style = MaterialTheme.typography.titleMedium
                )

                // Preview Card
                state.previewIdentity?.let { identity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header with global refresh button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.generator_preview),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Global refresh button with animation
                                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )

                                IconButton(
                                    onClick = { viewModel.refreshAll() },
                                    enabled = !state.isGenerating
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.generator_refresh),
                                        modifier = Modifier.rotate(if (state.isGenerating) rotation else 0f)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            // ─────────────────────────────────────────────────────────────
                            // NAME - with individual refresh
                            // ─────────────────────────────────────────────────────────────
                            IdentityFieldRow(
                                label = stringResource(R.string.field_name),
                                value = identity.fullName.fullDisplay,
                                icon = Icons.Default.Person,
                                isRefreshing = state.isRefreshingName,
                                onRefresh = { viewModel.refreshName() }
                            )

                            // ─────────────────────────────────────────────────────────────
                            // EMAIL - Clickable to select from imported aliases
                            // ─────────────────────────────────────────────────────────────
                            val emailValue = identity.email?.value
                            if (emailValue != null) {
                                // Email is configured and has a value
                                IdentityFieldRowWithSelection(
                                    label = stringResource(R.string.field_email),
                                    value = emailValue,
                                    icon = Icons.Default.Email,
                                    isRefreshing = state.isRefreshingEmail,
                                    hasMultipleOptions = state.existingAliases.size > 1,
                                    onRefresh = { viewModel.refreshEmail() },
                                    onClick = {
                                        // Open alias selection dialog if aliases available
                                        if (state.existingAliases.isNotEmpty()) {
                                            viewModel.showAliasSelectionDialog()
                                        }
                                    }
                                )
                            } else if (!state.isEmailConfigured) {
                                // Email not configured - show prompt to configure
                                ServiceNotConfiguredCard(
                                    icon = Icons.Outlined.Email,
                                    title = stringResource(R.string.email_not_configured_title),
                                    message = stringResource(R.string.email_not_configured_message),
                                    buttonText = stringResource(R.string.email_configure_button),
                                    onConfigureClick = onNavigateToAliasSettings
                                )
                            } else {
                                // Email configured but no alias selected yet
                                ServiceNotConfiguredCard(
                                    icon = Icons.Outlined.Email,
                                    title = stringResource(R.string.alias_select_prompt_title),
                                    message = stringResource(R.string.alias_select_prompt_message),
                                    buttonText = stringResource(R.string.alias_select_button),
                                    onConfigureClick = {
                                        viewModel.showAliasSelectionDialog()
                                    }
                                )
                            }

                            // ─────────────────────────────────────────────────────────────
                            // PHONE - Show field or "not configured" card
                            // ─────────────────────────────────────────────────────────────
                            val phoneValue = identity.phone
                            if (state.isPhoneConfigured && phoneValue.formatted.isNotBlank()) {
                                IdentityFieldRow(
                                    label = stringResource(R.string.field_phone),
                                    value = phoneValue.formatted,
                                    icon = Icons.Default.Phone,
                                    isRefreshing = state.isRefreshingPhone,
                                    onRefresh = { viewModel.refreshPhone() }
                                )
                            } else if (!state.isPhoneConfigured) {
                                // Phone not configured - show prompt
                                ServiceNotConfiguredCard(
                                    icon = Icons.Outlined.Phone,
                                    title = stringResource(R.string.phone_not_configured_title),
                                    message = stringResource(R.string.phone_not_configured_message),
                                    buttonText = stringResource(R.string.phone_configure_button),
                                    onConfigureClick = onNavigateToPhoneAliasSettings
                                )
                            } else {
                                // Phone configured but no value generated yet
                                IdentityFieldRow(
                                    label = stringResource(R.string.field_phone),
                                    value = phoneValue.formatted,
                                    icon = Icons.Default.Phone,
                                    isRefreshing = state.isRefreshingPhone,
                                    onRefresh = { viewModel.refreshPhone() }
                                )
                            }

                            // ─────────────────────────────────────────────────────────────
                            // DATE OF BIRTH - with individual refresh
                            // ─────────────────────────────────────────────────────────────
                            IdentityFieldRow(
                                label = stringResource(R.string.field_dob),
                                value = "${identity.dateOfBirth.displayFormat} (${stringResource(R.string.field_age)} ${identity.dateOfBirth.age})",
                                icon = Icons.Default.Cake,
                                isRefreshing = state.isRefreshingDob,
                                onRefresh = { viewModel.refreshDateOfBirth() }
                            )

                            // ─────────────────────────────────────────────────────────────
                            // ADDRESS (if included) - with individual refresh
                            // ─────────────────────────────────────────────────────────────
                            if (state.includeAddress && identity.address != null) {
                                IdentityFieldRow(
                                    label = stringResource(R.string.field_address),
                                    value = identity.address.displayFull,
                                    icon = Icons.Default.LocationOn,
                                    isRefreshing = state.isRefreshingAddress,
                                    onRefresh = { viewModel.refreshAddress() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveIdentity() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && state.previewIdentity != null
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.generator_save))
                    }
                }
            }
        }
    }
}

/**
 * Copy text to system clipboard.
 * Security: Text is copied as plain text. User explicitly requested copy action.
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AnonForge", text)
    clipboard.setPrimaryClip(clip)
}

/**
 * Reusable card shown when a service (email or phone) is not configured.
 * Prompts user to configure the service in Settings.
 */
@Composable
private fun ServiceNotConfiguredCard(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String,
    onConfigureClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )

            if (onConfigureClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onConfigureClick) {
                    Text(buttonText)
                }
            }
        }
    }
}

/**
 * Row displaying an identity field with individual refresh capability.
 */
@Composable
fun IdentityFieldRow(
    label: String,
    value: String,
    icon: ImageVector,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fieldRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fieldRotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(40.dp),
            enabled = !isRefreshing
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.generator_refresh),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isRefreshing) rotation else 0f)
            )
        }
    }
}

/**
 * Row displaying an identity field with selection capability.
 * Shows a dropdown indicator when multiple options are available (e.g., email aliases).
 * Clicking opens a selection dialog.
 */
@Composable
fun IdentityFieldRowWithSelection(
    label: String,
    value: String,
    icon: ImageVector,
    isRefreshing: Boolean = false,
    hasMultipleOptions: Boolean = false,
    onRefresh: () -> Unit,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fieldRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fieldRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasMultipleOptions) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Show dropdown indicator if multiple aliases available
                    if (hasMultipleOptions) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.alias_tap_to_change),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(40.dp),
            enabled = !isRefreshing
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.generator_refresh),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isRefreshing) rotation else 0f)
            )
        }
    }
}

@Composable
private fun getExpiryLabel(expiry: ExpiryDuration): String {
    return when (expiry) {
        ExpiryDuration.ONE_DAY -> stringResource(R.string.expiry_1d)
        ExpiryDuration.ONE_WEEK -> stringResource(R.string.expiry_1w)
        ExpiryDuration.ONE_MONTH -> stringResource(R.string.expiry_1m)
        ExpiryDuration.PERMANENT -> stringResource(R.string.expiry_permanent)
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