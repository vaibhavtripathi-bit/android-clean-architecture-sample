package com.vaibhav.restaurant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaibhav.restaurant.core.database.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT SUM(quantity) FROM cart_items")
    fun getCartItemCount(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE menuItemId = :menuItemId")
    suspend fun updateQuantity(menuItemId: String, quantity: Int)

    @Query("DELETE FROM cart_items WHERE menuItemId = :menuItemId")
    suspend fun removeCartItem(menuItemId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
