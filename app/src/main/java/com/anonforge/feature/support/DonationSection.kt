package com.anonforge.feature.support

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anonforge.R
import com.anonforge.domain.model.DonationTier

/**
 * Donation section with tier selection.
 *
 * FEATURES:
 * - Toggle between one-time and monthly donations
 * - 3 predefined tiers with animated selection
 * - Material3 design with subtle animations
 * - Full accessibility support
 *
 * PRIVACY:
 * - No data collected here
 * - All payment handled by Stripe
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationSection(
    isRecurring: Boolean,
    onRecurringToggle: (Boolean) -> Unit,
    selectedTierId: String?,
    onTierSelect: (String) -> Unit,
    onDonateClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════════
        // ONE-TIME / MONTHLY TOGGLE
        // ═══════════════════════════════════════════════════════════════════════
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Donation frequency selection" }
        ) {
            SegmentedButton(
                selected = !isRecurring,
                onClick = { onRecurringToggle(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text(stringResource(R.string.donation_onetime)) }
            )
            SegmentedButton(
                selected = isRecurring,
                onClick = { onRecurringToggle(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text(stringResource(R.string.donation_monthly)) }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // TIER CARDS
        // ═══════════════════════════════════════════════════════════════════════
        val tiers = if (isRecurring) {
            DonationTier.getMonthlyTiers()
        } else {
            DonationTier.getOneTimeTiers()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tiers.forEach { tier ->
                TierCard(
                    tier = tier,
                    isSelected = selectedTierId == tier.id,
                    isRecurring = isRecurring,
                    onClick = { onTierSelect(tier.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // DONATE BUTTON
        // ═══════════════════════════════════════════════════════════════════════
        Button(
            onClick = onDonateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedTierId != null && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = stringResource(R.string.donation_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Individual tier card with animated selection state.
 */
@Composable
private fun TierCard(
    tier: DonationTier,
    isSelected: Boolean,
    isRecurring: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated border and elevation
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "borderWidth"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        label = "borderColor"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    // Tier display data
    val tierName = when (tier) {
        is DonationTier.Coffee -> stringResource(R.string.donation_tier_coffee)
        is DonationTier.Lunch -> stringResource(R.string.donation_tier_lunch)
        is DonationTier.Champion -> stringResource(R.string.donation_tier_champion)
        is DonationTier.MonthlySupporter -> stringResource(R.string.donation_tier_supporter)
        is DonationTier.MonthlyPro -> stringResource(R.string.donation_tier_pro)
        is DonationTier.MonthlyHero -> stringResource(R.string.donation_tier_hero)
    }

    val emoji = when (tier) {
        is DonationTier.Coffee, is DonationTier.MonthlySupporter -> "☕"
        is DonationTier.Lunch, is DonationTier.MonthlyPro -> "🍽️"
        is DonationTier.Champion, is DonationTier.MonthlyHero -> "🏆"
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "$tierName ${tier.amountCents / 100} euros"
        },
        border = BorderStroke(width = borderWidth, color = borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji icon
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tier name
            Text(
                text = tierName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Amount
            Text(
                text = "${tier.amountCents / 100}€",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Monthly indicator
            if (isRecurring) {
                Text(
                    text = stringResource(R.string.donation_per_month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}