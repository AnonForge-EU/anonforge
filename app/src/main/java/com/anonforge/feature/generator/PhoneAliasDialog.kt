package com.anonforge.feature.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anonforge.R
import com.anonforge.domain.model.PhoneAlias

/**
 * Dialog for selecting or adding a phone alias during identity generation.
 *
 * FEATURES:
 * - Quick select from existing numbers
 * - Add new number inline
 * - Use primary number shortcut
 *
 * NO API INTEGRATION - all numbers are manually entered.
 */
@Composable
fun PhoneAliasDialog(
    existingAliases: List<PhoneAlias>,
    onSelectExisting: (PhoneAlias) -> Unit,
    onAddNew: (String) -> Unit,
    onUsePrimary: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAddInput by rememberSaveable { mutableStateOf(false) }
    var phoneInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.phone_alias_dialog_title)) },
        text = {
            Column {
                // ════════════════════════════════════════════════════════
                // ADD NEW NUMBER SECTION
                // ════════════════════════════════════════════════════════
                AnimatedVisibility(visible = !showAddInput) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddInput = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.phone_alias_option_new),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.phone_alias_option_new_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Inline input when adding new
                AnimatedVisibility(visible = showAddInput, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.phone_alias_add_hint)) },
                            placeholder = { Text("+33 6 12 34 56 78") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (phoneInput.isNotBlank()) {
                                        onAddNew(phoneInput.trim())
                                    }
                                }
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                showAddInput = false
                                phoneInput = ""
                            }) {
                                Text(stringResource(R.string.cancel))
                            }
                            TextButton(
                                onClick = { onAddNew(phoneInput.trim()) },
                                enabled = phoneInput.isNotBlank()
                            ) {
                                Text(stringResource(R.string.phone_alias_add_button))
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════
                // EXISTING NUMBERS
                // ════════════════════════════════════════════════════════
                if (existingAliases.isNotEmpty() && !showAddInput) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.phone_alias_existing_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(existingAliases) { alias ->
                            PhoneAliasItem(
                                alias = alias,
                                onClick = { onSelectExisting(alias) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                // Empty state
                if (existingAliases.isEmpty() && !showAddInput) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.phone_alias_history_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            // Use primary button (only if primary exists)
            existingAliases.find { it.isPrimary }?.let {
                TextButton(onClick = onUsePrimary) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.phone_alias_use_primary))
                }
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
private fun PhoneAliasItem(
    alias: PhoneAlias,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (alias.isPrimary) Icons.Default.Star else Icons.Default.Phone,
            contentDescription = if (alias.isPrimary) {
                stringResource(R.string.phone_alias_primary)
            } else null,
            tint = if (alias.isPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alias.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (alias.isPrimary) FontWeight.Medium else FontWeight.Normal
            )
            if (alias.friendlyName.isNotBlank()) {
                Text(
                    text = alias.friendlyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(R.string.phone_alias_used_count, alias.usageCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
