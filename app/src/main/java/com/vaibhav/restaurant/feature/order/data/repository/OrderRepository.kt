package com.vaibhav.restaurant.feature.order.data.repository

import com.vaibhav.restaurant.core.database.dao.OrderDao
import com.vaibhav.restaurant.core.database.entity.OrderEntity
import com.vaibhav.restaurant.feature.cart.domain.model.CartItem
import com.vaibhav.restaurant.feature.order.domain.model.Order
import com.vaibhav.restaurant.feature.order.domain.model.OrderItem
import com.vaibhav.restaurant.feature.order.domain.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SerializableOrderItem(
    val menuItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllOrders(): Flow<List<Order>> =
        orderDao.getAllOrders()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun getOrderById(orderId: String): Flow<Order?> =
        orderDao.getOrderById(orderId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    suspend fun placeOrder(cartItems: List<CartItem>, totalAmount: Double): String {
        val orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}"
        val orderItems = cartItems.map {
            SerializableOrderItem(it.menuItemId, it.name, it.price, it.quantity)
        }

        val entity = OrderEntity(
            id = orderId,
            items = json.encodeToString(orderItems),
            totalAmount = totalAmount,
            status = OrderStatus.PLACED.name,
            createdAt = System.currentTimeMillis(),
            estimatedDeliveryMinutes = 30
        )

        withContext(Dispatchers.IO) {
            orderDao.insertOrder(entity)
        }

        return orderId
    }

    /**
     * Simulates real-time order status updates using channelFlow.
     * channelFlow allows concurrent coroutines to send values into the flow.
     * This demonstrates how you'd bridge a real-time data source (e.g., WebSocket,
     * Firebase) into a Flow.
     */
    fun observeOrderStatus(orderId: String): Flow<OrderStatus> = channelFlow {
        val statuses = OrderStatus.entries

        launch {
            for (i in statuses.indices) {
                delay(3000L * (i + 1))
                send(statuses[i])

                withTimeout(5000) {
                    withContext(Dispatchers.IO) {
                        val entity = orderDao.getOrderById(orderId).first()
                        entity?.let {
                            orderDao.updateOrder(it.copy(status = statuses[i].name))
                        }
                    }
                }

                if (statuses[i] == OrderStatus.DELIVERED) break
            }
        }

        awaitClose()
    }

    /**
     * Alternative: callbackFlow demonstration.
     * Simulates a callback-based API (like a WebSocket listener) bridged to Flow.
     */
    fun observeOrderStatusViaCallback(orderId: String): Flow<OrderStatus> = callbackFlow {
        var currentIndex = 0
        val statuses = OrderStatus.entries

        val job = launch {
            while (currentIndex < statuses.size) {
                delay(4000)
                trySend(statuses[currentIndex])
                currentIndex++
            }
        }

        awaitClose { job.cancel() }
    }

    private fun OrderEntity.toDomain(): Order {
        val items = try {
            json.decodeFromString<List<SerializableOrderItem>>(this.items).map {
                OrderItem(it.menuItemId, it.name, it.price, it.quantity)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return Order(
            id = id,
            items = items,
            totalAmount = totalAmount,
            status = try { OrderStatus.valueOf(status) } catch (e: Exception) { OrderStatus.PLACED },
            createdAt = createdAt,
            estimatedDeliveryMinutes = estimatedDeliveryMinutes
        )
    }
}
