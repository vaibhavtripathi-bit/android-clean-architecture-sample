package com.vaibhav.restaurant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vaibhav.restaurant.core.database.converter.Converters
import com.vaibhav.restaurant.core.database.dao.CartDao
import com.vaibhav.restaurant.core.database.dao.OrderDao
import com.vaibhav.restaurant.core.database.entity.CartItemEntity
import com.vaibhav.restaurant.core.database.entity.OrderEntity

@Database(
    entities = [CartItemEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
}
