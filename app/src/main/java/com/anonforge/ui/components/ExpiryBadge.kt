package com.anonforge.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
@Suppress("unused")
@Composable
fun ExpiryBadge(expiresAt: Instant) {
    val now = Clock.System.now()
    val remaining = expiresAt - now
    
    val (color, text) = when {
        remaining < 0.hours -> 
            MaterialTheme.colorScheme.error to "Expired"
        remaining < 24.hours -> 
            MaterialTheme.colorScheme.error to "${remaining.inWholeHours}h"
        remaining < 7.days -> 
            MaterialTheme.colorScheme.tertiary to "${remaining.inWholeDays}d"
        else -> 
            MaterialTheme.colorScheme.primary to "${remaining.inWholeDays}d"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError
        )
    }
}
