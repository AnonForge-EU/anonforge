package com.anonforge.feature.support

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anonforge.R
import com.anonforge.domain.model.DonationResult
import com.anonforge.ui.components.SecureScreen

/**
 * Support & Why screen.
 * Provides donation options and educational content.
 *
 * PRIVACY: No tracking, no data collection. All donation handling via Stripe.
 * NO AFFILIATE LINKS - Donations only.
 *
 * Deep link handling: The SupportViewModel observes DeepLinkManager events,
 * so this screen automatically updates when returning from Stripe checkout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWhyPage: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle donation results (from WebView or external browser deep link)
    LaunchedEffect(state.donationResult) {
        when (val result = state.donationResult) {
            is DonationResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.donation_thank_you),
                    duration = SnackbarDuration.Long
                )
                viewModel.clearDonationResult()
            }
            is DonationResult.Cancelled -> {
                // Silent dismiss, no message needed
                viewModel.clearDonationResult()
            }
            is DonationResult.Error -> {
                // CONNECTED: Show actual error message from result
                val errorMessage = result.message.ifBlank {
                    context.getString(R.string.donation_error)
                }
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearDonationResult()
            }
            null -> { /* No action */ }
        }
    }

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.support_title)) },
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
                // ═══════════════════════════════════════════════════════════════
                // HEADER CARD
                // ═══════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.support_header_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.support_header_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Supporter badge
                        if (state.isSupporter) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuggestionChip(
                                onClick = { },
                                label = { Text(stringResource(R.string.supporter_badge)) },
                                icon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // WHY ANONFORGE SECTION
                // ═══════════════════════════════════════════════════════════════
                SectionHeader(stringResource(R.string.support_section_why))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWhyPage() },
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
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.support_why_title),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.support_why_subtitle),
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

                // ═══════════════════════════════════════════════════════════════
                // DONATIONS SECTION
                // ═══════════════════════════════════════════════════════════════
                SectionHeader(stringResource(R.string.support_section_donate))

                DonationSection(
                    isRecurring = state.isRecurringDonation,
                    onRecurringToggle = { viewModel.toggleRecurring(it) },
                    selectedTierId = state.selectedTierId,
                    onTierSelect = { viewModel.selectTier(it) },
                    onDonateClick = { viewModel.startDonation(context) },
                    isLoading = state.isLoading
                )

                // Security note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.donation_security_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ═══════════════════════════════════════════════════════════════
                // AFFILIATION SECTION REMOVED
                // Donations only - no affiliate links
                // ═══════════════════════════════════════════════════════════════

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHECKOUT WEBVIEW
    // ═══════════════════════════════════════════════════════════════════════════
    if (state.showCheckoutWebView && state.checkoutUrl != null) {
        DonationCheckoutSheet(
            checkoutUrl = state.checkoutUrl!!,
            onDismiss = { viewModel.dismissCheckout() },
            onSuccess = { viewModel.onDonationSuccess() },
            onCancel = { viewModel.onDonationCancelled() }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}