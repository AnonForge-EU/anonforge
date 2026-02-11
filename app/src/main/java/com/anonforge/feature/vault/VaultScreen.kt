package com.anonforge.feature.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.ui.components.IdentityCard
import com.anonforge.ui.components.LoadingSkeleton
import com.anonforge.ui.components.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateToGenerator: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle deleted message
    LaunchedEffect(state.deletedMessage) {
        state.deletedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeletedMessage()
        }
    }

    // Handle general snackbar messages (copy, rename)
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
                    title = { Text(stringResource(R.string.vault_title)) },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToGenerator,
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.vault_generate)
                        )
                    },
                    text = { Text(stringResource(R.string.vault_generate)) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    state.isLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(3) {
                                LoadingSkeleton()
                            }
                        }
                    }

                    state.identities.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBox,
                                contentDescription = "No identities",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.vault_empty_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.vault_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToGenerator) {
                                Icon(Icons.Default.Add, contentDescription = "Generate")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.vault_generate_first))
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.identities,
                                key = { it.id }
                            ) { identity ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300)) +
                                            expandVertically(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(300)) +
                                            shrinkVertically(animationSpec = tween(300))
                                ) {
                                    IdentityCard(
                                        identity = identity,
                                        isRevealed = viewModel.isRevealed(identity.id),
                                        onRevealToggle = { viewModel.toggleReveal(identity.id) },
                                        onDelete = { viewModel.deleteIdentity(identity.id) },
                                        onCopyField = { label, value ->
                                            viewModel.copyField(label, value)
                                        },
                                        onCopyAll = { fields ->
                                            viewModel.copyAllFields(fields)
                                        },
                                        onRename = { newName ->
                                            viewModel.renameIdentity(identity.id, newName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}