package com.vaibhav.restaurant.core.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Observes network connectivity changes using callbackFlow.
 * Demonstrates: callbackFlow, awaitClose, distinctUntilChanged
 */
interface ConnectivityObserver {
    val isConnected: Flow<Boolean>
}
