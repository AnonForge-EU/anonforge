package com.anonforge.feature.aliasimport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anonforge.R
import com.anonforge.domain.model.FetchAliasesResult
import com.anonforge.domain.model.ImportResult
import com.anonforge.domain.model.RemoteAlias

/**
 * Dialog for importing email aliases from SimpleLogin.
 *
 * NOTE: With Skill 17.2, phone numbers are managed manually (Hushed/OnOff).
 * This dialog only handles email alias import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasImportDialog(
    onDismiss: () -> Unit,
    emailResult: FetchAliasesResult?,
    isLoading: Boolean,
    importResult: ImportResult?,
    onFetchEmails: () -> Unit,
    onImport: (List<RemoteAlias.Email>) -> Unit,
    onRetry: () -> Unit
) {
    val selectedEmails = remember { mutableStateListOf<RemoteAlias.Email>() }

    LaunchedEffect(Unit) {
        onFetchEmails()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = { Text(stringResource(R.string.import_dialog_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        if (selectedEmails.isNotEmpty()) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${selectedEmails.size}")
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.import_tab_email),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                HorizontalDivider()

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    EmailAliasListContent(
                        result = emailResult,
                        isLoading = isLoading,
                        selectedItems = selectedEmails,
                        onToggle = { alias ->
                            if (selectedEmails.contains(alias)) {
                                selectedEmails.remove(alias)
                            } else {
                                selectedEmails.add(alias)
                            }
                        },
                        onSelectAll = {
                            (emailResult as? FetchAliasesResult.Success)?.aliases
                                ?.filterIsInstance<RemoteAlias.Email>()
                                ?.let {
                                    selectedEmails.clear()
                                    selectedEmails.addAll(it)
                                }
                        },
                        onDeselectAll = { selectedEmails.clear() },
                        onRetry = onRetry
                    )
                }

                // Import Result Banner
                AnimatedVisibility(visible = importResult != null) {
                    importResult?.let { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isFullSuccess) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (result.isFullSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.import_result_summary, result.successCount, result.duplicateCount))
                                    if (result.failureCount > 0) {
                                        Text(
                                            text = stringResource(R.string.import_result_failures, result.failureCount),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { onImport(selectedEmails.toList()) },
                            enabled = selectedEmails.isNotEmpty() && !isLoading
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.import_button, selectedEmails.size))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailAliasListContent(
    result: FetchAliasesResult?,
    isLoading: Boolean,
    selectedItems: List<RemoteAlias.Email>,
    onToggle: (RemoteAlias.Email) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading && result == null -> LoadingState()
        result is FetchAliasesResult.NotConfigured -> NotConfiguredState()
        result is FetchAliasesResult.Empty -> EmptyState()
        result is FetchAliasesResult.Error -> ErrorState(result, onRetry)
        result is FetchAliasesResult.Success -> {
            val aliases = result.aliases.filterIsInstance<RemoteAlias.Email>()
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with count and select buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.import_found_count, aliases.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = onSelectAll) {
                            Text(stringResource(R.string.import_select_all))
                        }
                        TextButton(onClick = onDeselectAll) {
                            Text(stringResource(R.string.import_deselect_all))
                        }
                    }
                }

                HorizontalDivider()

                // Alias list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(aliases, key = { it.id }) { alias ->
                        val isSelected = selectedItems.contains(alias)
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected,
                                    onValueChange = { onToggle(alias) },
                                    role = Role.Checkbox
                                ),
                            headlineContent = {
                                Text(
                                    text = alias.displayValue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                if (alias.forwardCount > 0) {
                                    Text(
                                        text = stringResource(R.string.import_forward_count, alias.forwardCount),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            leadingContent = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
        else -> LoadingState()
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.import_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.import_empty_email),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotConfiguredState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.import_not_configured_email),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_configure_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(error: FetchAliasesResult.Error, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (error.isRateLimited) Icons.Default.Timer else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (error.isRateLimited) {
                    stringResource(R.string.import_rate_limited)
                } else {
                    stringResource(R.string.import_error)
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error.retryAfterSeconds?.let { seconds ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.import_retry_after, seconds),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}