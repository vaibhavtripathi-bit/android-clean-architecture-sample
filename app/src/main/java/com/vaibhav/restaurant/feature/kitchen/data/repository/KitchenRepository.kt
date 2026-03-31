package com.vaibhav.restaurant.feature.kitchen.data.repository

import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketItem
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketPriority
import com.vaibhav.restaurant.feature.kitchen.domain.model.TicketStatus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class RawTicket(
    val id: String,
    val orderNumber: Int,
    val tableName: String,
    val items: List<TicketItem>,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAtMillis: Long,
    val estimatedPrepTimeSeconds: Int
)

@Singleton
class KitchenRepository @Inject constructor() {

    private val menuItems = listOf(
        "Margherita Pizza", "Pepperoni Pizza", "BBQ Chicken Pizza",
        "Caesar Salad", "Greek Salad", "Caprese Salad",
        "Grilled Salmon", "Beef Steak", "Chicken Parmesan",
        "Pasta Carbonara", "Spaghetti Bolognese", "Fettuccine Alfredo",
        "Spring Rolls", "Garlic Bread", "Bruschetta",
        "Chocolate Lava Cake", "Tiramisu", "Cheesecake",
        "Fish & Chips", "Lobster Bisque", "Tom Yum Soup",
        "Pad Thai", "Kung Pao Chicken", "Butter Chicken",
        "Lamb Chops", "Duck Confit", "Risotto", "Tacos", "Burrito Bowl"
    )

    private val specialNotes = listOf(
        "", "", "", "", "",
        "No onions", "Extra cheese", "Gluten free", "Spicy",
        "Dairy free", "Well done", "Medium rare", "No nuts",
        "Extra sauce", "Light salt"
    )

    private val tableNames = (1..30).map { "Table $it" } +
        listOf("Bar 1", "Bar 2", "Bar 3", "Patio 1", "Patio 2",
            "VIP 1", "VIP 2", "Takeout", "Delivery")

    private val _tickets = MutableStateFlow<List<RawTicket>>(emptyList())
    val tickets: Flow<List<RawTicket>> = _tickets.asStateFlow()

    suspend fun generateTickets(count: Int = 500) {
        delay(300)
        val now = System.currentTimeMillis()
        val generated = (1..count).map { i ->
            val itemCount = (1..5).random()
            val items = (1..itemCount).map {
                TicketItem(
                    name = menuItems.random(),
                    quantity = (1..3).random(),
                    specialNotes = specialNotes.random()
                )
            }.toImmutableList()

            val status = when {
                i <= count * 0.30 -> TicketStatus.QUEUED
                i <= count * 0.55 -> TicketStatus.IN_PROGRESS
                i <= count * 0.75 -> TicketStatus.ALMOST_DONE
                i <= count * 0.90 -> TicketStatus.READY
                else -> TicketStatus.SERVED
            }

            val priority = when {
                i % 20 == 0 -> TicketPriority.URGENT
                i % 7 == 0 -> TicketPriority.HIGH
                i % 3 == 0 -> TicketPriority.NORMAL
                else -> TicketPriority.LOW
            }

            val ageSeconds = (30..900).random()
            val estimatedPrep = (120..600).random()

            RawTicket(
                id = "TKT-${String.format("%04d", i)}",
                orderNumber = 1000 + i,
                tableName = tableNames.random(),
                items = items,
                status = status,
                priority = priority,
                createdAtMillis = now - (ageSeconds * 1000L),
                estimatedPrepTimeSeconds = estimatedPrep
            )
        }
        _tickets.value = generated
    }

    fun advanceTicketStatus(ticketId: String) {
        _tickets.update { list ->
            list.map { ticket ->
                if (ticket.id == ticketId) {
                    val nextStatus = when (ticket.status) {
                        TicketStatus.QUEUED -> TicketStatus.IN_PROGRESS
                        TicketStatus.IN_PROGRESS -> TicketStatus.ALMOST_DONE
                        TicketStatus.ALMOST_DONE -> TicketStatus.READY
                        TicketStatus.READY -> TicketStatus.SERVED
                        TicketStatus.SERVED -> TicketStatus.SERVED
                    }
                    ticket.copy(status = nextStatus)
                } else ticket
            }
        }
    }

    fun bumpPriority(ticketId: String) {
        _tickets.update { list ->
            list.map { ticket ->
                if (ticket.id == ticketId) {
                    val nextPriority = when (ticket.priority) {
                        TicketPriority.LOW -> TicketPriority.NORMAL
                        TicketPriority.NORMAL -> TicketPriority.HIGH
                        TicketPriority.HIGH -> TicketPriority.URGENT
                        TicketPriority.URGENT -> TicketPriority.URGENT
                    }
                    ticket.copy(priority = nextPriority)
                } else ticket
            }
        }
    }
}
