package com.vaibhav.restaurant.feature.kitchen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaibhav.restaurant.feature.kitchen.data.repository.KitchenRepository
import com.vaibhav.restaurant.feature.kitchen.data.repository.RawTicket
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenEvent
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenListItem
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenStats
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenTicket
import com.vaibhav.restaurant.feature.kitchen.domain.model.KitchenUiState
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketPriority
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Performance-critical ViewModel for the Kitchen Dashboard.
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. ALL computation happens here on Dispatchers.Default — the UI thread does ZERO math.
 *
 * 2. A ticker flow emits every 1 second. It is combined with the raw ticket data
 *    to produce pre-computed KitchenTicket objects with formatted strings, progress
 *    fractions, and overdue flags already calculated.
 *
 * 3. The output is ImmutableList<KitchenListItem> — Compose compiler can skip
 *    recomposition for items whose reference hasn't changed.
 *
 * 4. Items include section headers with contentType discrimination, allowing
 *    LazyColumn to reuse compose nodes efficiently (like RecyclerView viewTypes).
 *
 * 5. Stable keys (ticket.id) enable LazyColumn to diff items and only recompose
 *    cells that actually changed.
 *
 * 6. String formatting (timer display, item counts) is pre-computed here,
 *    not in @Composable functions — avoids per-frame allocations in the UI.
 */
@HiltViewModel
class KitchenViewModel @Inject constructor(
    private val repository: KitchenRepository
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<TicketStatus?>(null)
    private val _sortByPriority = MutableStateFlow(false)

    /**
     * Ticker flow: emits the current timestamp every second.
     * This drives the countdown timers for all 500 tickets.
     * Using flow + delay instead of a Timer avoids lifecycle leaks —
     * the flow is cancelled when viewModelScope is cancelled.
     */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    /**
     * Core pipeline:
     * ticker (1s) + rawTickets + filter + sort
     *   → compute all derived fields on Dispatchers.Default
     *   → produce ImmutableList<KitchenListItem>
     *   → stateIn as hot StateFlow
     */
    val uiState: StateFlow<KitchenUiState> = combine(
        repository.tickets,
        ticker,
        _filterStatus,
        _sortByPriority
    ) { rawTickets, now, filterStatus, sortByPriority ->
        computeUiState(rawTickets, now, filterStatus, sortByPriority)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KitchenUiState()
        )

    init {
        viewModelScope.launch {
            repository.generateTickets(500)
        }
    }

    fun onEvent(event: KitchenEvent) {
        when (event) {
            is KitchenEvent.FilterByStatus -> {
                _filterStatus.value = event.status
            }
            is KitchenEvent.TogglePrioritySort -> {
                _sortByPriority.value = !_sortByPriority.value
            }
            is KitchenEvent.AdvanceTicket -> {
                repository.advanceTicketStatus(event.ticketId)
            }
            is KitchenEvent.BumpPriority -> {
                repository.bumpPriority(event.ticketId)
            }
        }
    }

    /**
     * Pure function: all computation on Default dispatcher.
     * Produces the entire UI state from raw data + current time.
     *
     * This is where the performance magic happens:
     * - Pre-formats all timer strings ("3:45", "OVERDUE 1:20")
     * - Pre-computes progress fractions (0.0f..1.0f)
     * - Pre-computes overdue flags
     * - Pre-computes item count display strings
     * - Groups tickets by status with section headers
     * - Sorts within groups by priority if enabled
     * - Wraps everything in @Immutable data classes + ImmutableList
     */
    private fun computeUiState(
        rawTickets: List<RawTicket>,
        now: Long,
        filterStatus: TicketStatus?,
        sortByPriority: Boolean
    ): KitchenUiState {
        if (rawTickets.isEmpty()) return KitchenUiState(isLoading = true)

        val activeTickets = rawTickets.filter { it.status != TicketStatus.SERVED }

        val stats = computeStats(activeTickets, now)

        val filtered = if (filterStatus != null) {
            activeTickets.filter { it.status == filterStatus }
        } else {
            activeTickets
        }

        val sorted = if (sortByPriority) {
            filtered.sortedWith(
                compareByDescending<RawTicket> { it.priority.ordinal }
                    .thenBy { it.createdAtMillis }
            )
        } else {
            filtered.sortedBy { it.createdAtMillis }
        }

        val computedTickets = sorted.map { raw -> computeTicket(raw, now) }

        val listItems = buildListItems(computedTickets, filterStatus)

        return KitchenUiState(
            listItems = listItems,
            stats = stats,
            isLoading = false,
            filterStatus = filterStatus,
            sortByPriority = sortByPriority
        )
    }

    private fun computeTicket(raw: RawTicket, now: Long): KitchenTicket {
        val elapsedSeconds = ((now - raw.createdAtMillis) / 1000).toInt().coerceAtLeast(0)
        val remainingSeconds = (raw.estimatedPrepTimeSeconds - elapsedSeconds).coerceAtLeast(0)
        val isOverdue = elapsedSeconds > raw.estimatedPrepTimeSeconds
        val overdueSeconds = if (isOverdue) elapsedSeconds - raw.estimatedPrepTimeSeconds else 0

        val progressFraction = if (raw.estimatedPrepTimeSeconds > 0) {
            (elapsedSeconds.toFloat() / raw.estimatedPrepTimeSeconds).coerceIn(0f, 1.5f)
        } else 1f

        val remainingTimeFormatted = if (isOverdue) {
            "OVERDUE ${formatTime(overdueSeconds)}"
        } else {
            formatTime(remainingSeconds)
        }

        val totalItems = raw.items.sumOf { it.quantity }
        val itemCountDisplay = "${raw.items.size} dish${if (raw.items.size != 1) "es" else ""}, $totalItems item${if (totalItems != 1) "s" else ""}"

        return KitchenTicket(
            id = raw.id,
            orderNumber = raw.orderNumber,
            tableName = raw.tableName,
            items = raw.items.toImmutableList(),
            status = raw.status,
            priority = raw.priority,
            createdAtMillis = raw.createdAtMillis,
            estimatedPrepTimeSeconds = raw.estimatedPrepTimeSeconds,
            elapsedSeconds = elapsedSeconds,
            progressFraction = progressFraction,
            remainingTimeFormatted = remainingTimeFormatted,
            isOverdue = isOverdue,
            itemCountDisplay = itemCountDisplay
        )
    }

    private fun computeStats(activeTickets: List<RawTicket>, now: Long): KitchenStats {
        val queued = activeTickets.count { it.status == TicketStatus.QUEUED }
        val inProgress = activeTickets.count { it.status == TicketStatus.IN_PROGRESS }
        val almostDone = activeTickets.count { it.status == TicketStatus.ALMOST_DONE }
        val ready = activeTickets.count { it.status == TicketStatus.READY }

        val overdueCount = activeTickets.count { raw ->
            val elapsed = ((now - raw.createdAtMillis) / 1000).toInt()
            elapsed > raw.estimatedPrepTimeSeconds
        }

        val avgWaitSeconds = if (activeTickets.isNotEmpty()) {
            activeTickets.map { ((now - it.createdAtMillis) / 1000).toInt() }.average().toInt()
        } else 0

        return KitchenStats(
            totalActive = activeTickets.size,
            queued = queued,
            inProgress = inProgress,
            almostDone = almostDone,
            ready = ready,
            overdueCount = overdueCount,
            avgWaitFormatted = formatTime(avgWaitSeconds)
        )
    }

    /**
     * Groups tickets by status and inserts section headers.
     * Each item gets a stableId for LazyColumn key-based diffing.
     */
    private fun buildListItems(
        tickets: List<KitchenTicket>,
        filterStatus: TicketStatus?
    ): ImmutableList<KitchenListItem> {
        if (filterStatus != null) {
            return tickets.map { KitchenListItem.Ticket(it, it.id) }.toImmutableList()
        }

        val result = mutableListOf<KitchenListItem>()
        val grouped = tickets.groupBy { it.status }

        val displayOrder = listOf(
            TicketStatus.QUEUED,
            TicketStatus.IN_PROGRESS,
            TicketStatus.ALMOST_DONE,
            TicketStatus.READY
        )

        for (status in displayOrder) {
            val group = grouped[status] ?: continue
            if (group.isEmpty()) continue
            result.add(
                KitchenListItem.Header(
                    title = status.displayName,
                    count = group.size,
                    stableId = "header_${status.name}"
                )
            )
            result.addAll(group.map { KitchenListItem.Ticket(it, it.id) })
        }

        return result.toImmutableList()
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${String.format("%02d", seconds)}"
    }
}
