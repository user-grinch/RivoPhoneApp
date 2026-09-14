package com.grinch.rivo4.view.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.AnalyticsTimeRange
import com.grinch.rivo4.controller.CallAnalyticsSummary
import com.grinch.rivo4.controller.CallAnalyticsViewModel
import com.grinch.rivo4.controller.TopContactStat
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoLoadingIndicatorView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallAnalyticsScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: CallAnalyticsViewModel = koinViewModel()
    val analytics by viewModel.analytics.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Analytics & Tracking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (isLoading && analytics.totalCalls == 0) {
            RivoLoadingIndicatorView(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time Range Segmented Control
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnalyticsTimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = selectedRange == range,
                                onClick = { viewModel.setTimeRange(range) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AnalyticsTimeRange.entries.size)
                            ) {
                                Text(range.label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Hero Talk Time Banner
                item {
                    HeroTalkTimeCard(analytics = analytics)
                }

                // Call Distribution Summary Grid
                item {
                    CallDistributionCard(analytics = analytics)
                }

                // Most Talked Person Leaderboard
                item {
                    Text(
                        text = "Most Talked Persons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                if (analytics.topContacts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.PhoneDisabled, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("No call records found for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    itemsIndexed(analytics.topContacts) { index, contact ->
                        TopContactLeaderboardItem(
                            rank = index + 1,
                            contact = contact,
                            onClick = {
                                // Navigate to details if contact has number/id
                            }
                        )
                    }
                }

                // SIM Card Usage (if multiple SIMs found)
                if (analytics.simUsage.size > 1) {
                    item {
                        RivoExpressiveCard(
                            title = "SIM Usage Breakdown",
                            icon = Icons.Outlined.SimCard
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                analytics.simUsage.forEach { (sim, durationSec) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sim, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(formatAnalyticsDuration(durationSec), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HeroTalkTimeCard(analytics: CallAnalyticsSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Total Talk Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatAnalyticsDuration(analytics.totalTalkTimeSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroMetricItem(label = "Total Calls", value = "${analytics.totalCalls}")
                HeroMetricItem(label = "Avg Duration", value = formatShortDuration(analytics.avgDurationSeconds))
                HeroMetricItem(label = "Missed", value = "${analytics.missedCalls}")
            }
        }
    }
}

@Composable
private fun HeroMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CallDistributionCard(analytics: CallAnalyticsSummary) {
    RivoExpressiveCard(
        title = "Call Breakdown",
        icon = Icons.Outlined.PieChart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val total = analytics.totalCalls.coerceAtLeast(1).toFloat()
            val inRatio = analytics.incomingCalls / total
            val outRatio = analytics.outgoingCalls / total
            val missRatio = (analytics.missedCalls + analytics.rejectedCalls) / total

            // Stacked progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (inRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(inRatio)
                            .fillMaxHeight()
                            .background(Color(0xFF2E7D32)) // Green
                    )
                }
                if (outRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(outRatio)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (missRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(missRatio)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BreakdownLegend(
                    color = Color(0xFF2E7D32),
                    label = "Incoming",
                    count = analytics.incomingCalls
                )
                BreakdownLegend(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Outgoing",
                    count = analytics.outgoingCalls
                )
                BreakdownLegend(
                    color = MaterialTheme.colorScheme.error,
                    label = "Missed",
                    count = analytics.missedCalls + analytics.rejectedCalls
                )
            }
        }
    }
}

@Composable
private fun BreakdownLegend(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopContactLeaderboardItem(
    rank: Int,
    contact: TopContactStat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                shape = CircleShape,
                color = when (rank) {
                    1 -> Color(0xFFFFD700).copy(alpha = 0.25f) // Gold
                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.25f) // Silver
                    3 -> Color(0xFFCD7F32).copy(alpha = 0.25f) // Bronze
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (rank) {
                            1 -> Color(0xFFB8860B)
                            2 -> Color(0xFF708090)
                            3 -> Color(0xFF8B4513)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            RivoAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
                modifier = Modifier.size(44.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${contact.totalCalls} calls (${contact.incomingCount} in, ${contact.outgoingCount} out)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAnalyticsDuration(contact.totalDurationSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "talked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatAnalyticsDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${secs}s"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

fun formatShortDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
