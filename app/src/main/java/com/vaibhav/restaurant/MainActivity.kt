package com.vaibhav.restaurant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.vaibhav.restaurant.core.connectivity.ConnectivityObserver
import com.vaibhav.restaurant.core.datastore.UserPreferences
import com.vaibhav.restaurant.navigation.AppNavGraph
import com.vaibhav.restaurant.navigation.Route
import com.vaibhav.restaurant.ui.theme.RestaurantTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isConnected by connectivityObserver.isConnected
                .collectAsStateWithLifecycle(initialValue = true)

            val isDarkMode by userPreferences.userPreferencesFlow
                .collectAsStateWithLifecycle(
                    initialValue = com.vaibhav.restaurant.core.datastore.UserPreferencesData()
                )

            RestaurantTheme(darkTheme = isDarkMode.isDarkMode, dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val isLoggedIn by userPreferences.isLoggedIn
                        .collectAsStateWithLifecycle(initialValue = false)

                    val startDestination: Route = if (isLoggedIn) Route.Home else Route.Login

                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )

                    // Connectivity banner using callbackFlow-backed ConnectivityObserver
                    AnimatedVisibility(
                        visible = !isConnected,
                        enter = slideInVertically(),
                        exit = slideOutVertically(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error)
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = "No internet connection",
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
