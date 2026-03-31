package com.vaibhav.restaurant.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

sealed interface ProfileEvent {
    data class ToggleDarkMode(val enabled: Boolean) : ProfileEvent
    data class ToggleNotifications(val enabled: Boolean) : ProfileEvent
    data object LogoutClicked : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * DataStore preferences exposed as Flow, transformed via map,
     * then converted to hot StateFlow via stateIn.
     */
    val uiState: StateFlow<ProfileUiState> = userPreferences.userPreferencesFlow
        .map { prefs ->
            ProfileUiState(
                userName = prefs.userName,
                userEmail = prefs.userEmail,
                isDarkMode = prefs.isDarkMode,
                notificationsEnabled = prefs.notificationsEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ToggleDarkMode -> {
                viewModelScope.launch {
                    userPreferences.setDarkMode(event.enabled)
                }
            }
            is ProfileEvent.ToggleNotifications -> {
                viewModelScope.launch {
                    userPreferences.setNotificationsEnabled(event.enabled)
                }
            }
            is ProfileEvent.LogoutClicked -> {
                viewModelScope.launch {
                    userPreferences.logout()
                    _effects.send(UiEffect.Navigate("login"))
                }
            }
        }
    }
}
