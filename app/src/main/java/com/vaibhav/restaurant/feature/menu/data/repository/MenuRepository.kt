package com.vaibhav.restaurant.feature.menu.data.repository

import com.vaibhav.restaurant.core.network.FakeRestaurantApi
import com.vaibhav.restaurant.feature.menu.domain.model.Category
import com.vaibhav.restaurant.feature.menu.domain.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val api: FakeRestaurantApi
) {
    fun getCategories(): Flow<List<Category>> = flow {
        val dtos = api.getCategories()
        emit(dtos.map { Category(it.id, it.name, it.imageUrl) })
    }.flowOn(Dispatchers.IO)

    fun getMenuItems(): Flow<List<MenuItem>> = flow {
        val dtos = api.getMenuItems()
        emit(dtos.map { MenuItem(it.id, it.categoryId, it.name, it.description, it.price, it.imageUrl, it.isAvailable) })
    }.flowOn(Dispatchers.IO)

    fun getMenuItem(id: String): Flow<MenuItem> = flow {
        val dto = api.getMenuItem(id)
        emit(MenuItem(dto.id, dto.categoryId, dto.name, dto.description, dto.price, dto.imageUrl, dto.isAvailable))
    }.flowOn(Dispatchers.IO)

    /**
     * Demonstrates supervisorScope + async for parallel data loading.
     * Both categories and menu items are fetched concurrently.
     */
    fun getCategoriesAndItems(): Flow<Pair<List<Category>, List<MenuItem>>> = flow {
        val result = supervisorScope {
            val categoriesDeferred = async { api.getCategories() }
            val itemsDeferred = async { api.getMenuItems() }

            val categories = categoriesDeferred.await().map {
                Category(it.id, it.name, it.imageUrl)
            }
            val items = itemsDeferred.await().map {
                MenuItem(it.id, it.categoryId, it.name, it.description, it.price, it.imageUrl, it.isAvailable)
            }
            categories to items
        }
        emit(result)
    }.flowOn(Dispatchers.IO)
}
