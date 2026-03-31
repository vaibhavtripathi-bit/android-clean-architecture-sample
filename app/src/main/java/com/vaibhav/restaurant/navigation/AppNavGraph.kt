package com.vaibhav.restaurant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vaibhav.restaurant.feature.auth.presentation.LoginScreen
import com.vaibhav.restaurant.feature.auth.presentation.RegisterScreen
import com.vaibhav.restaurant.feature.cart.presentation.CartScreen
import com.vaibhav.restaurant.feature.menu.presentation.HomeScreen
import com.vaibhav.restaurant.feature.menu.presentation.MenuItemDetailScreen
import com.vaibhav.restaurant.feature.order.presentation.OrderHistoryScreen
import com.vaibhav.restaurant.feature.order.presentation.OrderTrackingScreen
import com.vaibhav.restaurant.feature.kitchen.presentation.KitchenDashboardScreen
import com.vaibhav.restaurant.feature.profile.presentation.ProfileScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Login> {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Route.Register) },
                onNavigateToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Register> {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Home> {
            HomeScreen(
                onNavigateToDetail = { itemId ->
                    navController.navigate(Route.MenuItemDetail(itemId))
                },
                onNavigateToCart = { navController.navigate(Route.Cart) },
                onNavigateToOrders = { navController.navigate(Route.OrderHistory) },
                onNavigateToProfile = { navController.navigate(Route.Profile) },
                onNavigateToKitchen = { navController.navigate(Route.KitchenDashboard) }
            )
        }

        composable<Route.MenuItemDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MenuItemDetail>()
            MenuItemDetailScreen(
                itemId = route.itemId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Route.Cart) }
            )
        }

        composable<Route.Cart> {
            CartScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTracking = { orderId ->
                    navController.navigate(Route.OrderTracking(orderId)) {
                        popUpTo(Route.Home)
                    }
                }
            )
        }

        composable<Route.OrderTracking> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.OrderTracking>()
            OrderTrackingScreen(
                orderId = route.orderId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.OrderHistory> {
            OrderHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTracking = { orderId ->
                    navController.navigate(Route.OrderTracking(orderId))
                }
            )
        }

        composable<Route.Profile> {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.KitchenDashboard> {
            KitchenDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
