package com.anonforge.feature.settings.alias

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import kotlinx.coroutines.flow.collectLatest

/**
 * Alias Settings Screen with secure API key management.
 *
 * SECURITY FEATURES:
 * - API key is NEVER displayed in plaintext
 * - Masked display shows "••••••••••••••••"
 * - Key hint shows only first 3 chars (e.g., "sl_...")
 * - Visibility toggle does NOT reveal stored key
 * - Input is cleared immediately after save
 *
 * WCAG AA COMPLIANCE:
 * - Proper content descriptions for all interactive elements
 * - Sufficient color contrast
 * - Touch targets >= 48dp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    viewModel: AliasSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val toggleClickSource = remember { MutableInteractionSource() }

    // Input state for new API key entry
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var showApiKeyInput by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryConfirmation by rememberSaveable { mutableStateOf(false) }

    // Dialog dismiss callbacks
    val dismissClearDialog: () -> Unit = { showClearConfirmation = false }
    val dismissClearHistoryDialog: () -> Unit = { showClearHistoryConfirmation = false }

    // Localized strings for snackbar messages
    val apiKeySavedMessage = stringResource(R.string.alias_key_saved_success)
    val apiKeyRemovedMessage = stringResource(R.string.alias_key_removed)
    val apiKeyInvalidMessage = stringResource(R.string.alias_key_invalid)
    val aliasSyncUpToDateMessage = stringResource(R.string.alias_sync_uptodate)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AliasSettingsEvent.ApiKeySaved -> {
                    // SECURITY: Clear input immediately after save
                    apiKeyInput = ""
                    showApiKeyInput = false
                    snackbarHostState.showSnackbar(apiKeySavedMessage)
                }

                is AliasSettingsEvent.ApiKeyCleared -> {
                    apiKeyInput = ""
                    showApiKeyInput = false
                    showClearConfirmation = false
                    snackbarHostState.showSnackbar(apiKeyRemovedMessage)
                }

                is AliasSettingsEvent.ValidationComplete -> {
                    if (!event.isValid) snackbarHostState.showSnackbar(apiKeyInvalidMessage)
                }

                is AliasSettingsEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is AliasSettingsEvent.SyncComplete -> {
                    val message = if (event.count > 0) {
                        context.resources.getQuantityString(
                            R.plurals.alias_sync_count,
                            event.count,
                            event.count
                        )
                    } else {
                        aliasSyncUpToDateMessage
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Reset editing state when key is saved
    LaunchedEffect(state.isEditingKey) {
        if (!state.isEditingKey) {
            apiKeyInput = ""
            showApiKeyInput = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alias_settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.navigate_back)
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
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
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.alias_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.alias_info_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Enable Toggle
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.isEnabled,
                            enabled = state.hasApiKey,
                            role = Role.Switch,
                            interactionSource = toggleClickSource,
                            indication = LocalIndication.current,
                            onValueChange = { enabled: Boolean ->
                                viewModel.toggleEnabled(enabled)
                            }
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.alias_enable_label),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (state.hasApiKey) {
                                    stringResource(R.string.alias_enable_description)
                                } else {
                                    stringResource(R.string.alias_configure_first)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = null,
                        enabled = state.hasApiKey
                    )
                }
            }

            // ════════════════════════════════════════════════════════════════════
            // HISTORY & IMPORT ACTIONS (visible when API is configured)
            // ════════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = state.hasApiKey,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.alias_management_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // History button
                        Card(
                            onClick = onNavigateToHistory,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.alias_history_title),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.alias_history_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Show alias count badge
                                if (state.aliasCount > 0) {
                                    Text(
                                        text = state.aliasCount.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Import button
                        Card(
                            onClick = onNavigateToImport,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.alias_import_title),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.alias_import_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ════════════════════════════════════════════════════════
                        // SYNC BUTTON - Syncs aliases from SimpleLogin
                        // ════════════════════════════════════════════════════════
                        Card(
                            onClick = { viewModel.syncAliases() },
                            enabled = !state.isSyncing,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.alias_sync_title),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (state.isSyncing) {
                                            stringResource(R.string.alias_sync_in_progress)
                                        } else {
                                            stringResource(R.string.alias_sync_subtitle)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // ════════════════════════════════════════════════════════
                        // CLEAR HISTORY BUTTON - Dangerous action with confirmation
                        // ════════════════════════════════════════════════════════
                        if (state.aliasCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                onClick = { showClearHistoryConfirmation = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.alias_clear_history_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.alias_clear_history_subtitle,
                                                state.aliasCount
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════════
            // API Configuration (expanded when enabled OR when editing)
            // ════════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = state.isEnabled || !state.hasApiKey || state.isEditingKey,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.alias_api_config_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ════════════════════════════════════════════════════════
                        // CONFIGURED STATE: Show masked key with actions
                        // ════════════════════════════════════════════════════════
                        if (state.hasApiKey && !state.isEditingKey) {
                            // Status Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (state.isValid) {
                                        true -> MaterialTheme.colorScheme.primaryContainer
                                        false -> MaterialTheme.colorScheme.errorContainer
                                        null -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        when {
                                            state.isValidating -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            state.isValid == true -> {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = stringResource(R.string.alias_key_status_valid),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            state.isValid == false -> {
                                                Icon(
                                                    Icons.Filled.Warning,
                                                    contentDescription = stringResource(R.string.alias_key_status_invalid),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = when {
                                                state.isValidating -> stringResource(R.string.alias_key_validating)
                                                state.isValid == true -> stringResource(R.string.alias_key_valid)
                                                state.isValid == false -> stringResource(R.string.alias_key_invalid)
                                                else -> stringResource(R.string.alias_key_configured)
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // SECURITY: Show masked display
                                    Text(
                                        text = state.maskedDisplay.ifEmpty { "••••••••••••••••" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics {
                                            contentDescription = context.getString(R.string.alias_key_masked_description)
                                        }
                                    )

                                    // Show hint if available
                                    state.keyHint?.let { hint ->
                                        Text(
                                            text = stringResource(R.string.alias_key_hint_format, hint),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Quota info
                                    state.quota?.let { quota ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.DataUsage,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.alias_quota_display,
                                                    quota.used,
                                                    quota.totalAllowed
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (quota.isPremium) {
                                                AssistChip(
                                                    onClick = { },
                                                    label = {
                                                        Text(
                                                            text = "Premium",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.startEditingKey() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.alias_change_key))
                                }

                                OutlinedButton(
                                    onClick = { showClearConfirmation = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.remove))
                                }
                            }

                            // Test connection button
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.testConnection() },
                                enabled = !state.isValidating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.alias_test_connection))
                            }
                        }

                        // ════════════════════════════════════════════════════════
                        // INPUT STATE: Show text field for new/replacement key
                        // ════════════════════════════════════════════════════════
                        if (!state.hasApiKey || state.isEditingKey) {
                            // Input field for API key
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = context.getString(R.string.alias_api_key_label)
                                    },
                                enabled = !state.isValidating,
                                label = { Text(stringResource(R.string.alias_api_key_label)) },
                                placeholder = { Text("sl_...") },
                                singleLine = true,
                                // SECURITY: Always mask input by default
                                visualTransformation = if (showApiKeyInput) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showApiKeyInput = !showApiKeyInput },
                                        modifier = Modifier.semantics {
                                            contentDescription = if (showApiKeyInput) {
                                                context.getString(R.string.alias_api_key_hide)
                                            } else {
                                                context.getString(R.string.alias_api_key_show)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (showApiKeyInput) {
                                                Icons.Filled.VisibilityOff
                                            } else {
                                                Icons.Filled.Visibility
                                            },
                                            contentDescription = null
                                        )
                                    }
                                },
                                supportingText = { Text(stringResource(R.string.alias_api_key_hint)) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cancel button (only when replacing existing key)
                                if (state.isEditingKey) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelEditingKey() },
                                        enabled = !state.isValidating
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }

                                // Save & Validate button
                                val canSave = apiKeyInput.isNotBlank() && !state.isValidating
                                Button(
                                    onClick = { viewModel.saveApiKey(apiKeyInput.trim()) },
                                    modifier = Modifier.weight(1f),
                                    enabled = canSave
                                ) {
                                    if (state.isValidating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(stringResource(R.string.alias_save_key))
                                }
                            }
                        }
                    }
                }
            }

            // Signup Link
            Card(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://simplelogin.io".toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.alias_signup_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.alias_signup_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Security Note
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.alias_security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Clear API Key Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = dismissClearDialog,
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.alias_clear_title)) },
            text = { Text(stringResource(R.string.alias_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearApiKey() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.alias_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissClearDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = dismissClearHistoryDialog,
            icon = {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.alias_clear_history_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.alias_clear_history_dialog_message,
                        state.aliasCount
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAliasHistory()
                        dismissClearHistoryDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.alias_clear_history_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissClearHistoryDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}