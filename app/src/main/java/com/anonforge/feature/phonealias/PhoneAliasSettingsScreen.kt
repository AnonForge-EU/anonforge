package com.anonforge.feature.phonealias

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.domain.model.PhoneAlias
import com.anonforge.ui.components.SecureScreen

/**
 * Phone Alias Settings Screen - Manual Input Mode.
 *
 * FEATURES:
 * - Manual entry of virtual phone numbers
 * - Links to external services (Hushed, OnOff, TextNow)
 * - Local encrypted storage for reuse
 * - Set primary number for quick selection
 *
 * NO API INTEGRATION - user obtains numbers from external services.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAliasSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhoneAliasSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    @Suppress("SpellCheckingInspection") // "snackbar" is correct Material Design terminology
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    // Input state
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }

    // Localized messages
    val copiedMessage = stringResource(R.string.phone_alias_copied)
    val addedMessage = stringResource(R.string.phone_alias_added_success)
    val deletedMessage = stringResource(R.string.phone_alias_deleted_success)
    val primaryUpdatedMessage = stringResource(R.string.phone_alias_primary_updated)
    val invalidFormatMessage = stringResource(R.string.phone_alias_invalid_format)
    val alreadyExistsMessage = stringResource(R.string.phone_alias_already_exists)

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.phone_alias_settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
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
                // ════════════════════════════════════════════════════════════════
                // INFO CARD
                // ════════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.phone_alias_info_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.phone_alias_info_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // ENABLE TOGGLE
                // ════════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.phone_alias_enable_label),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.phone_alias_enable_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { viewModel.toggleEnabled(it) },
                            enabled = state.aliases.isNotEmpty()
                        )
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // ADD NUMBER SECTION
                // ════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.phone_alias_add_title),
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
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.phone_alias_add_hint)) },
                            placeholder = { Text("+33 6 12 34 56 78") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.PhoneAndroid,
                                    contentDescription = null
                                )
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.phone_alias_name_hint)) },
                            placeholder = { Text(stringResource(R.string.phone_alias_name_example)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (phoneInput.isNotBlank()) {
                                        viewModel.addPhoneAlias(
                                            phoneNumber = phoneInput.trim(),
                                            friendlyName = nameInput.trim(),
                                            onSuccess = addedMessage,
                                            onInvalidFormat = invalidFormatMessage,
                                            onAlreadyExists = alreadyExistsMessage
                                        )
                                        phoneInput = ""
                                        nameInput = ""
                                    }
                                }
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.addPhoneAlias(
                                    phoneNumber = phoneInput.trim(),
                                    friendlyName = nameInput.trim(),
                                    onSuccess = addedMessage,
                                    onInvalidFormat = invalidFormatMessage,
                                    onAlreadyExists = alreadyExistsMessage
                                )
                                phoneInput = ""
                                nameInput = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = phoneInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.phone_alias_add_button))
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // EXTERNAL SERVICES
                // ════════════════════════════════════════════════════════════════
                Text(
                    text = stringResource(R.string.phone_alias_services_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.phone_alias_services_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Service: Hushed
                ServiceCard(
                    name = stringResource(R.string.phone_alias_service_hushed),
                    description = stringResource(R.string.phone_alias_service_hushed_desc),
                    onClick = { uriHandler.openUri("https://hushed.com") }
                )

                // Service: OnOff
                ServiceCard(
                    name = stringResource(R.string.phone_alias_service_onoff),
                    description = stringResource(R.string.phone_alias_service_onoff_desc),
                    onClick = { uriHandler.openUri("https://www.onoff.app") }
                )

                // Service: TextNow
                ServiceCard(
                    name = stringResource(R.string.phone_alias_service_textnow),
                    description = stringResource(R.string.phone_alias_service_textnow_desc),
                    onClick = { uriHandler.openUri("https://www.textnow.com") }
                )

                // ════════════════════════════════════════════════════════════════
                // SAVED NUMBERS
                // ════════════════════════════════════════════════════════════════
                if (state.aliases.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.phone_alias_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (state.aliases.size > 1) {
                            TextButton(onClick = { viewModel.showClearDialog() }) {
                                Text(
                                    text = stringResource(R.string.clear),
                                    color = MaterialTheme.colorScheme.error
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
                        Column(modifier = Modifier.padding(8.dp)) {
                            state.aliases.forEachIndexed { index, alias ->
                                PhoneAliasHistoryItem(
                                    alias = alias,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(alias.phoneNumber))
                                        viewModel.showMessage(copiedMessage)
                                    },
                                    onDelete = { viewModel.showDeleteDialog(alias) },
                                    onSetPrimary = {
                                        viewModel.setPrimary(alias.id, primaryUpdatedMessage)
                                    }
                                )
                                if (index < state.aliases.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                } else {
                    // Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.phone_alias_history_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.phone_alias_history_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════════
                // SECURITY NOTE
                // ════════════════════════════════════════════════════════════════
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
                            text = stringResource(R.string.phone_alias_security_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // DIALOGS
        // ════════════════════════════════════════════════════════════════════════

        // Clear All Dialog
        if (state.showClearDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearDialog() },
                title = { Text(stringResource(R.string.phone_alias_clear_title)) },
                text = { Text(stringResource(R.string.phone_alias_clear_message)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearAllAliases() }) {
                        Text(
                            stringResource(R.string.remove),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissClearDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Delete Single Alias Dialog
        state.aliasToDelete?.let { alias ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text(stringResource(R.string.phone_alias_delete_title)) },
                text = {
                    Text(stringResource(R.string.phone_alias_delete_message, alias.phoneNumber))
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteAlias(alias.id, deletedMessage) }) {
                        Text(
                            stringResource(R.string.remove),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ServiceCard(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.phone_alias_open_service),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PhoneAliasHistoryItem(
    alias: PhoneAlias,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSetPrimary,
            enabled = !alias.isPrimary
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = stringResource(R.string.phone_alias_set_primary),
                tint = if (alias.isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alias.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (alias.isPrimary) FontWeight.Medium else FontWeight.Normal
            )
            Row {
                if (alias.friendlyName.isNotBlank()) {
                    Text(
                        text = alias.friendlyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.phone_alias_used_count, alias.usageCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.action_copy)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}