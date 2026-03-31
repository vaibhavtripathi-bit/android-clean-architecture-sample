package com.vaibhav.restaurant.feature.auth.data.repository

import com.vaibhav.restaurant.core.datastore.UserPreferences
import com.vaibhav.restaurant.core.network.FakeRestaurantApi
import com.vaibhav.restaurant.feature.auth.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: FakeRestaurantApi,
    private val userPreferences: UserPreferences
) {
    fun login(email: String, password: String): Flow<User> = flow {
        val userDto = api.login(email, password)
        val user = User(
            id = userDto.id,
            name = userDto.name,
            email = userDto.email,
            phone = userDto.phone
        )
        userPreferences.saveUser(user.id, user.name, user.email)
        emit(user)
    }

    fun register(name: String, email: String, password: String): Flow<User> = flow {
        val userDto = api.register(name, email, password)
        val user = User(
            id = userDto.id,
            name = userDto.name,
            email = userDto.email,
            phone = userDto.phone
        )
        userPreferences.saveUser(user.id, user.name, user.email)
        emit(user)
    }
}
