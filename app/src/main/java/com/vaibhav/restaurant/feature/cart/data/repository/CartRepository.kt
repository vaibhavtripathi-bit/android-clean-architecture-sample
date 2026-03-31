package com.vaibhav.restaurant.feature.cart.data.repository

import com.vaibhav.restaurant.core.database.dao.CartDao
import com.vaibhav.restaurant.core.database.entity.CartItemEntity
import com.vaibhav.restaurant.feature.cart.domain.model.CartItem
import com.vaibhav.restaurant.feature.menu.domain.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demonstrates:
 * - Room DAO returning Flow<List<Entity>> for reactive data
 * - map operator to transform entities to domain models
 * - Mutex for thread-safe cart mutations
 * - flowOn(Dispatchers.IO) for context switching
 */
@Singleton
class CartRepository @Inject constructor(
    private val cartDao: CartDao
) {
    private val mutex = Mutex()

    fun getCartItems(): Flow<List<CartItem>> =
        cartDao.getAllCartItems()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)

    fun getCartItemCount(): Flow<Int> =
        cartDao.getCartItemCount()
            .map { it ?: 0 }
            .flowOn(Dispatchers.IO)

    suspend fun addToCart(item: MenuItem, quantity: Int) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                cartDao.upsertCartItem(
                    CartItemEntity(
                        menuItemId = item.id,
                        name = item.name,
                        price = item.price,
                        imageUrl = item.imageUrl,
                        quantity = quantity
                    )
                )
            }
        }
    }

    suspend fun updateQuantity(menuItemId: String, quantity: Int) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (quantity <= 0) {
                    cartDao.removeCartItem(menuItemId)
                } else {
                    cartDao.updateQuantity(menuItemId, quantity)
                }
            }
        }
    }

    suspend fun removeItem(menuItemId: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                cartDao.removeCartItem(menuItemId)
            }
        }
    }

    suspend fun clearCart() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                cartDao.clearCart()
            }
        }
    }

    private fun CartItemEntity.toDomain() = CartItem(
        menuItemId = menuItemId,
        name = name,
        price = price,
        imageUrl = imageUrl,
        quantity = quantity
    )
}
