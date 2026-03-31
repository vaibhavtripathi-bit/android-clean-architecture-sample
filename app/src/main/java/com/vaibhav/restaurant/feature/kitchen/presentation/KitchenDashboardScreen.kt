package com.vaibhav.restaurant.feature.kitchen.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenEvent
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenListItem
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenStats
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenTicket
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenUiState
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketPriority
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketStatus

private enum class ListContentType { HEADER, TICKET }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KitchenDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: KitchenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(KitchenEvent.TogglePrioritySort) }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Sort by priority",
                            tint = if (uiState.sortByPriority) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                StatsBar(stats = uiState.stats)

                FilterRow(
                    selectedStatus = uiState.filterStatus,
                    onFilterSelected = { viewModel.onEvent(KitchenEvent.FilterByStatus(it)) }
                )

                /**
                 * HIGH-PERFORMANCE LazyColumn:
                 *
                 * 1. key = stableId → enables O(1) diffing, items move without recreating nodes
                 * 2. contentType → LazyColumn reuses compose nodes of the same type
                 *    (headers reuse header slots, tickets reuse ticket slots)
                 * 3. All data is @Immutable + ImmutableList → Compose skips recomposition
                 *    for items whose reference hasn't changed
                 * 4. No lambda allocations in items() — callbacks use remember
                 */
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.listItems,
                        key = { it.stableId },
                        contentType = { item ->
                            when (item) {
                                is KitchenListItem.Header -> ListContentType.HEADER
                                is KitchenListItem.Ticket -> ListContentType.TICKET
                            }
                        }
                    ) { item ->
                        when (item) {
                            is KitchenListItem.Header -> {
                                SectionHeader(title = item.title, count = item.count)
                            }
                            is KitchenListItem.Ticket -> {
                                val onAdvance = remember(item.ticket.id) {
                                    { viewModel.onEvent(KitchenEvent.AdvanceTicket(item.ticket.id)) }
                                }
                                val onBump = remember(item.ticket.id) {
                                    { viewModel.onEvent(KitchenEvent.BumpPriority(item.ticket.id)) }
                                }
                                TicketCard(
                                    ticket = item.ticket,
                                    onAdvanceStatus = onAdvance,
                                    onBumpPriority = onBump
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBar(stats: KitchenStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Active", value = "${stats.totalActive}")
            StatItem(label = "Queued", value = "${stats.queued}")
            StatItem(label = "Cooking", value = "${stats.inProgress}")
            StatItem(label = "Ready", value = "${stats.ready}")
            StatItem(
                label = "Overdue",
                value = "${stats.overdueCount}",
                isAlert = stats.overdueCount > 0
            )
            StatItem(label = "Avg Wait", value = stats.avgWaitFormatted)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, isAlert: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    selectedStatus: TicketStatus?,
    onFilterSelected: (TicketStatus?) -> Unit
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onFilterSelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        TicketStatus.entries.filter { it != TicketStatus.SERVED }.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onFilterSelected(status) },
                label = { Text(status.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Individual ticket card — the most recomposed composable.
 *
 * PERFORMANCE NOTES:
 * - All displayed strings (timer, item count) are pre-computed in ViewModel
 * - animateFloatAsState for progress bar avoids recomposition — only the drawing changes
 * - animateColorAsState for overdue indicator is a single-value animation
 * - No String.format, no .sumOf, no .map inside this composable
 * - The ticket parameter is @Immutable — if the reference is the same, Compose skips entirely
 */
@Composable
private fun TicketCard(
    ticket: KitchenTicket,
    onAdvanceStatus: () -> Unit,
    onBumpPriority: () -> Unit
) {
    val priorityColor = remember(ticket.priority) {
        when (ticket.priority) {
            TicketPriority.URGENT -> Color(0xFFD32F2F)
            TicketPriority.HIGH -> Color(0xFFF57C00)
            TicketPriority.NORMAL -> Color(0xFF1976D2)
            TicketPriority.LOW -> Color(0xFF757575)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = ticket.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    val timerColor by animateColorAsState(
        targetValue = if (ticket.isOverdue) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "timerColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${ticket.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ticket.tableName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ticket.isOverdue) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = timerColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ticket.remainingTimeFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = timerColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (ticket.isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ticket.items.joinToString(", ") { "${it.quantity}x ${it.name}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ticket.itemCountDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ticket.items.firstOrNull { it.specialNotes.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it.specialNotes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Row {
                    if (ticket.priority != TicketPriority.URGENT) {
                        IconButton(
                            onClick = onBumpPriority,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PriorityHigh,
                                contentDescription = "Bump priority",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (ticket.status != TicketStatus.SERVED) {
                        IconButton(
                            onClick = onAdvanceStatus,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "Advance status",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
