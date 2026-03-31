package com.vaibhav.restaurant.core.network

import com.vaibhav.restaurant.core.network.dto.CategoryDto
import com.vaibhav.restaurant.core.network.dto.MenuItemDto
import com.vaibhav.restaurant.core.network.dto.UserDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRestaurantApi @Inject constructor() {

    suspend fun login(email: String, password: String): UserDto {
        delay(1000)
        if (email == "test@test.com" && password == "password") {
            return UserDto(
                id = "user_1",
                name = "John Doe",
                email = email,
                phone = "+1234567890"
            )
        }
        throw Exception("Invalid credentials")
    }

    suspend fun register(name: String, email: String, password: String): UserDto {
        delay(1500)
        return UserDto(
            id = "user_${System.currentTimeMillis()}",
            name = name,
            email = email,
            phone = ""
        )
    }

    suspend fun getCategories(): List<CategoryDto> {
        delay(800)
        return listOf(
            CategoryDto("cat_1", "Starters", "https://picsum.photos/seed/starters/200"),
            CategoryDto("cat_2", "Main Course", "https://picsum.photos/seed/main/200"),
            CategoryDto("cat_3", "Pizzas", "https://picsum.photos/seed/pizza/200"),
            CategoryDto("cat_4", "Burgers", "https://picsum.photos/seed/burger/200"),
            CategoryDto("cat_5", "Desserts", "https://picsum.photos/seed/dessert/200"),
            CategoryDto("cat_6", "Beverages", "https://picsum.photos/seed/drinks/200")
        )
    }

    suspend fun getMenuItems(): List<MenuItemDto> {
        delay(1000)
        return listOf(
            MenuItemDto("item_1", "cat_1", "Spring Rolls", "Crispy vegetable spring rolls with sweet chili sauce", 8.99, "https://picsum.photos/seed/springrolls/400/300", true),
            MenuItemDto("item_2", "cat_1", "Garlic Bread", "Toasted bread with garlic butter and herbs", 5.99, "https://picsum.photos/seed/garlicbread/400/300", true),
            MenuItemDto("item_3", "cat_1", "Soup of the Day", "Chef's special soup served with crusty bread", 7.49, "https://picsum.photos/seed/soup/400/300", true),
            MenuItemDto("item_4", "cat_2", "Grilled Salmon", "Atlantic salmon with lemon butter sauce and vegetables", 22.99, "https://picsum.photos/seed/salmon/400/300", true),
            MenuItemDto("item_5", "cat_2", "Chicken Parmesan", "Breaded chicken with marinara sauce and mozzarella", 18.99, "https://picsum.photos/seed/chickenparm/400/300", true),
            MenuItemDto("item_6", "cat_2", "Beef Steak", "8oz ribeye steak with mashed potatoes and gravy", 28.99, "https://picsum.photos/seed/steak/400/300", true),
            MenuItemDto("item_7", "cat_2", "Pasta Carbonara", "Spaghetti with creamy egg sauce, pancetta, and parmesan", 16.99, "https://picsum.photos/seed/carbonara/400/300", true),
            MenuItemDto("item_8", "cat_3", "Margherita Pizza", "Classic tomato, mozzarella, and fresh basil", 14.99, "https://picsum.photos/seed/margherita/400/300", true),
            MenuItemDto("item_9", "cat_3", "Pepperoni Pizza", "Loaded with pepperoni and mozzarella cheese", 16.99, "https://picsum.photos/seed/pepperoni/400/300", true),
            MenuItemDto("item_10", "cat_3", "BBQ Chicken Pizza", "Grilled chicken, BBQ sauce, red onions, and cilantro", 17.99, "https://picsum.photos/seed/bbqpizza/400/300", false),
            MenuItemDto("item_11", "cat_4", "Classic Burger", "Beef patty with lettuce, tomato, and special sauce", 13.99, "https://picsum.photos/seed/classicburger/400/300", true),
            MenuItemDto("item_12", "cat_4", "Cheese Burger", "Double patty with cheddar cheese and pickles", 15.99, "https://picsum.photos/seed/cheeseburger/400/300", true),
            MenuItemDto("item_13", "cat_5", "Chocolate Lava Cake", "Warm chocolate cake with molten center", 9.99, "https://picsum.photos/seed/lavacake/400/300", true),
            MenuItemDto("item_14", "cat_5", "Tiramisu", "Classic Italian coffee-flavored dessert", 8.99, "https://picsum.photos/seed/tiramisu/400/300", true),
            MenuItemDto("item_15", "cat_5", "Cheesecake", "New York style cheesecake with berry compote", 9.49, "https://picsum.photos/seed/cheesecake/400/300", true),
            MenuItemDto("item_16", "cat_6", "Fresh Lemonade", "Freshly squeezed lemonade with mint", 4.99, "https://picsum.photos/seed/lemonade/400/300", true),
            MenuItemDto("item_17", "cat_6", "Iced Coffee", "Cold brew coffee with cream", 5.49, "https://picsum.photos/seed/icedcoffee/400/300", true),
            MenuItemDto("item_18", "cat_6", "Mango Smoothie", "Fresh mango blended with yogurt", 6.99, "https://picsum.photos/seed/mangosmoothie/400/300", true)
        )
    }

    suspend fun getMenuItem(id: String): MenuItemDto {
        delay(500)
        return getMenuItems().first { it.id == id }
    }
}
