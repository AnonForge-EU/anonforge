package com.anonforge.feature.support

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anonforge.R
import com.anonforge.ui.components.SecureScreen

/**
 * Educational page explaining why AnonForge exists.
 * Covers data breaches, service trade-offs, and privacy policy.
 *
 * NOTE: This is purely educational content - NO affiliate links.
 * Services are mentioned objectively to help users make informed decisions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyAnonForgePage(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.why_page_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ═══════════════════════════════════════════════════════════════
                // HERO SECTION - Data Breach Warning
                // ═══════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.why_hero_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.why_hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Check your data button (HIBP)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, HIBP_URL.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.why_check_hibp))
                }

                // ═══════════════════════════════════════════════════════════════
                // WHY USE DISPOSABLE IDENTITIES
                // ═══════════════════════════════════════════════════════════════
                SectionTitle(stringResource(R.string.why_section_why))

                ReasonCard(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.why_reason1_title),
                    description = stringResource(R.string.why_reason1_desc)
                )

                ReasonCard(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.why_reason2_title),
                    description = stringResource(R.string.why_reason2_desc)
                )

                ReasonCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = stringResource(R.string.why_reason3_title),
                    description = stringResource(R.string.why_reason3_desc)
                )

                // ═══════════════════════════════════════════════════════════════
                // SERVICE TRADE-OFFS (Educational - No affiliate links)
                // ═══════════════════════════════════════════════════════════════
                SectionTitle(stringResource(R.string.why_section_services))

                // Email Services - SimpleLogin
                ServiceCard(
                    serviceName = "SimpleLogin",
                    serviceIcon = Icons.Default.Email,
                    pros = listOf(
                        stringResource(R.string.why_simplelogin_pro1),
                        stringResource(R.string.why_simplelogin_pro2),
                        stringResource(R.string.why_simplelogin_pro3)
                    ),
                    cons = listOf(
                        stringResource(R.string.why_simplelogin_con1),
                        stringResource(R.string.why_simplelogin_con2)
                    )
                )

                // Phone Services Section Header
                Text(
                    text = stringResource(R.string.why_phone_services_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = stringResource(R.string.why_phone_services_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Hushed Pros/Cons
                ServiceCard(
                    serviceName = "Hushed",
                    serviceIcon = Icons.Default.Phone,
                    pros = listOf(
                        stringResource(R.string.why_hushed_pro1),
                        stringResource(R.string.why_hushed_pro2),
                        stringResource(R.string.why_hushed_pro3)
                    ),
                    cons = listOf(
                        stringResource(R.string.why_hushed_con1),
                        stringResource(R.string.why_hushed_con2)
                    )
                )

                // OnOff Pros/Cons
                ServiceCard(
                    serviceName = "OnOff",
                    serviceIcon = Icons.Default.PhoneAndroid,
                    pros = listOf(
                        stringResource(R.string.why_onoff_pro1),
                        stringResource(R.string.why_onoff_pro2),
                        stringResource(R.string.why_onoff_pro3)
                    ),
                    cons = listOf(
                        stringResource(R.string.why_onoff_con1),
                        stringResource(R.string.why_onoff_con2)
                    )
                )

                // ═══════════════════════════════════════════════════════════════
                // PRIVACY POLICY
                // ═══════════════════════════════════════════════════════════════
                SectionTitle(stringResource(R.string.why_section_privacy))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PrivacyPoint(
                            icon = Icons.Default.CloudOff,
                            text = stringResource(R.string.why_privacy_point1)
                        )
                        PrivacyPoint(
                            icon = Icons.Default.Storage,
                            text = stringResource(R.string.why_privacy_point2)
                        )
                        PrivacyPoint(
                            icon = Icons.Default.Analytics,
                            text = stringResource(R.string.why_privacy_point3)
                        )
                        PrivacyPoint(
                            icon = Icons.Default.Share,
                            text = stringResource(R.string.why_privacy_point4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ReasonCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    serviceName: String,
    serviceIcon: ImageVector,
    pros: List<String>,
    cons: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = serviceIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pros
            Text(
                text = stringResource(R.string.why_pros),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            pros.forEach { pro ->
                ProConItem(
                    text = pro,
                    isPro = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cons
            Text(
                text = stringResource(R.string.why_cons),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            cons.forEach { con ->
                ProConItem(
                    text = con,
                    isPro = false
                )
            }
        }
    }
}

@Composable
private fun ProConItem(
    text: String,
    isPro: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPro) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isPro) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PrivacyPoint(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Have I Been Pwned - Data breach checker
@Suppress("SpellCheckingInspection")
private const val HIBP_URL = "https://haveibeenpwned.com/"