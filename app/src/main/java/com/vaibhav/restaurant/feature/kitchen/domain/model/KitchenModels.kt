package com.vaibhav.restaurant.feature.kitchen.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class TicketPriority { LOW, NORMAL, HIGH, URGENT }

enum class TicketStatus(val displayName: String) {
    QUEUED("Queued"),
    IN_PROGRESS("In Progress"),
    ALMOST_DONE("Almost Done"),
    READY("Ready"),
    SERVED("Served")
}

/**
 * @Immutable tells the Compose compiler this class will not change after creation.
 * Combined with ImmutableList, this prevents unnecessary recompositions because
 * Compose can skip recomposition when the reference hasn't changed.
 *
 * Every field is a primitive or an @Immutable/Stable type — no mutable collections.
 */
@Immutable
data class KitchenTicket(
    val id: String,
    val orderNumber: Int,
    val tableName: String,
    val items: ImmutableList<TicketItem>,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAtMillis: Long,
    val estimatedPrepTimeSeconds: Int,
    val elapsedSeconds: Int,
    val progressFraction: Float,
    val remainingTimeFormatted: String,
    val isOverdue: Boolean,
    val itemCountDisplay: String
)

@Immutable
data class TicketItem(
    val name: String,
    val quantity: Int,
    val specialNotes: String
)

/**
 * Represents a single cell in the LazyColumn.
 * Using a sealed interface with contentType discrimination lets LazyColumn
 * reuse ViewHolders (compose nodes) across items of the same type.
 */
@Immutable
sealed interface KitchenListItem {
    val stableId: String

    @Immutable
    data class Header(
        val title: String,
        val count: Int,
        override val stableId: String
    ) : KitchenListItem

    @Immutable
    data class Ticket(
        val ticket: KitchenTicket,
        override val stableId: String
    ) : KitchenListItem
}

@Immutable
data class KitchenStats(
    val totalActive: Int = 0,
    val queued: Int = 0,
    val inProgress: Int = 0,
    val almostDone: Int = 0,
    val ready: Int = 0,
    val overdueCount: Int = 0,
    val avgWaitFormatted: String = "0:00"
)

@Immutable
data class KitchenUiState(
    val listItems: ImmutableList<KitchenListItem> = persistentListOf(),
    val stats: KitchenStats = KitchenStats(),
    val isLoading: Boolean = true,
    val filterStatus: TicketStatus? = null,
    val sortByPriority: Boolean = false
)

sealed interface KitchenEvent {
    data class FilterByStatus(val status: TicketStatus?) : KitchenEvent
    data object TogglePrioritySort : KitchenEvent
    data class AdvanceTicket(val ticketId: String) : KitchenEvent
    data class BumpPriority(val ticketId: String) : KitchenEvent
}
