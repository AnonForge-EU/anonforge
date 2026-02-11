package com.anonforge.feature.generator.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anonforge.R

/**
 * Source of an alias - used for grouping in selection dialog.
 */
enum class AliasSource {
    IMPORTED,   // From SimpleLogin import
    GENERATED   // Generated via API during identity creation
}

/**
 * Represents a selectable alias in the dialog.
 */
data class SelectableAlias(
    val id: String,
    val value: String,
    val displayName: String?,
    val source: AliasSource,
    val useCount: Int = 0,
    val isEnabled: Boolean = true
)

/**
 * Alias selection dialog showing imported + generated aliases.
 * Skill 17: Enhanced to show imported aliases with grouping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasSelectionDialog(
    title: String,
    aliases: List<SelectableAlias>,
    isLoading: Boolean,
    onSelect: (SelectableAlias) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredAliases = remember(aliases, searchQuery) {
        if (searchQuery.isBlank()) {
            aliases
        } else {
            aliases.filter {
                it.value.contains(searchQuery, ignoreCase = true) ||
                        it.displayName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    val groupedAliases = remember(filteredAliases) {
        filteredAliases.groupBy { it.source }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                )

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.alias_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true
                )

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        filteredAliases.isEmpty() && searchQuery.isNotEmpty() -> {
                            // No search results
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(stringResource(R.string.alias_no_results))
                                }
                            }
                        }

                        filteredAliases.isEmpty() -> {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(stringResource(R.string.alias_empty_title))
                                    Spacer(Modifier.height(24.dp))
                                    FilledTonalButton(onClick = onCreateNew) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.alias_create_new))
                                    }
                                }
                            }
                        }

                        else -> {
                            // Alias list with sections
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                // Create New option
                                item {
                                    ListItem(
                                        modifier = Modifier.clickable(onClick = onCreateNew),
                                        headlineContent = {
                                            Text(
                                                text = stringResource(R.string.alias_create_new),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        supportingContent = {
                                            Text(stringResource(R.string.alias_create_new_hint))
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }

                                // Imported aliases section
                                groupedAliases[AliasSource.IMPORTED]?.let { importedList ->
                                    item {
                                        SectionHeader(
                                            title = stringResource(R.string.alias_section_imported),
                                            count = importedList.size,
                                            icon = Icons.Default.CloudDownload
                                        )
                                    }
                                    items(importedList, key = { "imported_${it.id}" }) { alias ->
                                        AliasListItem(
                                            alias = alias,
                                            onClick = { onSelect(alias) },
                                            leadingIcon = Icons.Default.CloudDone
                                        )
                                    }
                                    item { Spacer(Modifier.height(16.dp)) }
                                }

                                // Generated aliases section
                                groupedAliases[AliasSource.GENERATED]?.let { generatedList ->
                                    item {
                                        SectionHeader(
                                            title = stringResource(R.string.alias_section_generated),
                                            count = generatedList.size,
                                            icon = Icons.Default.AutoAwesome
                                        )
                                    }
                                    items(generatedList, key = { "generated_${it.id}" }) { alias ->
                                        AliasListItem(
                                            alias = alias,
                                            onClick = { onSelect(alias) },
                                            leadingIcon = Icons.Default.History
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom hint
                if (aliases.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.alias_selection_hint),
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

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text("$count")
        }
    }
}

@Composable
private fun AliasListItem(
    alias: SelectableAlias,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ListItem(
        modifier = Modifier.clickable(enabled = alias.isEnabled, onClick = onClick),
        headlineContent = {
            Text(
                text = alias.value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (alias.isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                alias.displayName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (alias.useCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(stringResource(R.string.alias_used_times, alias.useCount))
                    }
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}