package com.anonforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anonforge.R
import com.anonforge.domain.model.DomainIdentity

/**
 * Identity card component with reveal, copy, rename and delete functionality.
 * Supports per-field copy with auto-clear clipboard (Skill 13).
 * Handles nullable email field (Skill 14).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdentityCard(
    identity: DomainIdentity,
    isRevealed: Boolean,
    onRevealToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopyField: (label: String, value: String) -> Unit,
    onCopyAll: (fields: Map<String, String>) -> Unit,
    onRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog state - read in if() conditions and written in callbacks
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Display name: customName if set, otherwise full name
    val displayName = identity.customName?.takeIf { it.isNotBlank() }
        ?: identity.fullName.fullDisplay

    // Initials for avatar
    val customNameValue = identity.customName
    val initials = if (!customNameValue.isNullOrBlank()) {
        customNameValue.split(" ")
            .take(2)
            .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    } else {
        "${identity.fullName.firstName.firstOrNull()?.uppercaseChar() ?: ""}${identity.fullName.lastName.firstOrNull()?.uppercaseChar() ?: ""}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRevealToggle,
                onLongClick = { showRenameDialog = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with avatar, name, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar with initials
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Show original name if customName is set
                        if (identity.customName != null) {
                            Text(
                                text = identity.fullName.fullDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Expiry indicator
                        identity.expiresAt?.let {
                            val isExpired = identity.isExpired
                            Text(
                                text = if (isExpired)
                                    stringResource(R.string.identity_expired)
                                else
                                    stringResource(R.string.identity_expires_soon),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExpired)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    // Rename button
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Rename identity"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_rename),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Delete identity"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Reveal/Hide toggle
                    IconButton(
                        onClick = onRevealToggle,
                        modifier = Modifier.semantics {
                            contentDescription = if (isRevealed) "Hide identity details" else "Reveal identity details"
                        }
                    ) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isRevealed)
                                stringResource(R.string.action_hide)
                            else
                                stringResource(R.string.action_reveal),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Revealed content
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Name field
                    CopyableFieldRow(
                        icon = Icons.Default.Person,
                        label = stringResource(R.string.field_name),
                        value = identity.fullName.fullDisplay,
                        onCopy = { onCopyField(it, identity.fullName.fullDisplay) }
                    )

                    // Email field (only if present)
                    identity.email?.let { email ->
                        CopyableFieldRow(
                            icon = Icons.Default.Email,
                            label = stringResource(R.string.field_email),
                            value = email.value,
                            onCopy = { onCopyField(it, email.value) }
                        )
                    }

                    // Phone field
                    CopyableFieldRow(
                        icon = Icons.Default.Phone,
                        label = stringResource(R.string.field_phone),
                        value = identity.phone.formatted,
                        onCopy = { onCopyField(it, identity.phone.formatted) }
                    )

                    // Date of birth
                    CopyableFieldRow(
                        icon = Icons.Default.Cake,
                        label = stringResource(R.string.field_dob),
                        value = identity.dateOfBirth.displayFormat,
                        onCopy = { onCopyField(it, identity.dateOfBirth.displayFormat) }
                    )

                    // Address (if present)
                    identity.address?.let { address ->
                        CopyableFieldRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.field_address),
                            value = address.displayFull,
                            onCopy = { onCopyField(it, address.displayFull) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Copy All button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val fields = mutableMapOf(
                                    "Name" to identity.fullName.fullDisplay,
                                    "Phone" to identity.phone.formatted,
                                    "DOB" to identity.dateOfBirth.displayFormat
                                )
                                // Add email only if present
                                identity.email?.let { email ->
                                    fields["Email"] = email.value
                                }
                                identity.address?.let { address ->
                                    fields["Address"] = address.displayFull
                                }
                                onCopyAll(fields)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CopyAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_copy_all))
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        RenameIdentityDialog(
            currentName = identity.customName ?: identity.fullName.fullDisplay,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_identity_title)) },
            text = { Text(stringResource(R.string.delete_identity_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        // Note: showDeleteDialog state cleanup happens when parent removes this component
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * A single field row with copy button.
 */
@Composable
private fun CopyableFieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: (label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = { onCopy(label) },
            modifier = Modifier
                .size(36.dp)
                .semantics { contentDescription = "Copy $label" }
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.action_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dialog for renaming an identity.
 */
@Composable
fun RenameIdentityDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    // Validation state - read by OutlinedTextField.isError, written in callbacks
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rename_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { input ->
                        if (input.length <= 30) {
                            newName = input
                            isError = false
                        }
                    },
                    label = { Text(stringResource(R.string.rename_dialog_label)) },
                    isError = isError,
                    singleLine = true,
                    supportingText = {
                        Text("${newName.length}/30")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}