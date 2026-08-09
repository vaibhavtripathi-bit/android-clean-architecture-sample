package com.vaibhav.restaurant.feature.cart.data.repository

import com.vaibhav.restaurant.core.database.dao.CartDao
import com.vaibhav.restaurant.core.database.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory CartDao used only in tests, so CartRepository's Mutex/flowOn/map
 * logic can be verified without Room or an instrumented test.
 */
class FakeCartDao : CartDao {

    private val itemsFlow = MutableStateFlow<List<CartItemEntity>>(emptyList())

    override fun getAllCartItems(): Flow<List<CartItemEntity>> = itemsFlow

    override fun getCartItemCount(): Flow<Int?> =
        itemsFlow.map { items -> items.sumOf { it.quantity } }

    override suspend fun upsertCartItem(item: CartItemEntity) {
        itemsFlow.update { current ->
            val index = current.indexOfFirst { it.menuItemId == item.menuItemId }
            if (index >= 0) current.toMutableList().apply { set(index, item) }
            else current + item
        }
    }

    override suspend fun updateQuantity(menuItemId: String, quantity: Int) {
        itemsFlow.update { current ->
            current.map { if (it.menuItemId == menuItemId) it.copy(quantity = quantity) else it }
        }
    }

    override suspend fun removeCartItem(menuItemId: String) {
        itemsFlow.update { current -> current.filterNot { it.menuItemId == menuItemId } }
    }

    override suspend fun clearCart() {
        itemsFlow.value = emptyList()
    }
}
