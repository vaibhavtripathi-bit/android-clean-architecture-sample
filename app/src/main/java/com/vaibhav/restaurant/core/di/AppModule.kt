package com.vaibhav.restaurant.core.di

import android.content.Context
import androidx.room.Room
import com.vaibhav.restaurant.core.connectivity.ConnectivityObserver
import com.vaibhav.restaurant.core.connectivity.NetworkConnectivityObserver
import com.vaibhav.restaurant.core.database.RestaurantDatabase
import com.vaibhav.restaurant.core.database.dao.CartDao
import com.vaibhav.restaurant.core.database.dao.OrderDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(
        impl: NetworkConnectivityObserver
    ): ConnectivityObserver

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): RestaurantDatabase {
            return Room.databaseBuilder(
                context,
                RestaurantDatabase::class.java,
                "restaurant_db"
            ).build()
        }

        @Provides
        fun provideCartDao(database: RestaurantDatabase): CartDao = database.cartDao()

        @Provides
        fun provideOrderDao(database: RestaurantDatabase): OrderDao = database.orderDao()
    }
}
