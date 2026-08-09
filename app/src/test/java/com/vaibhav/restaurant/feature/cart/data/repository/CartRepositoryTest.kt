package com.vaibhav.restaurant.feature.cart.data.repository

import com.vaibhav.restaurant.feature.cart.domain.model.CartItem
import com.vaibhav.restaurant.feature.menu.domain.model.MenuItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {

    private lateinit var dao: FakeCartDao
    private lateinit var repository: CartRepository

    @Before
    fun setUp() {
        dao = FakeCartDao()
        repository = CartRepository(dao)
    }

    private fun menuItem(id: String, price: Double = 10.0) = MenuItem(
        id = id,
        categoryId = "cat-1",
        name = "Item $id",
        description = "",
        price = price,
        imageUrl = "",
        isAvailable = true
    )

    @Test
    fun `addToCart maps MenuItem into a CartItem`() = runTest {
        repository.addToCart(menuItem("m1", price = 12.0), quantity = 2)

        val items = repository.getCartItems().first()

        assertEquals(1, items.size)
        assertEquals(
            CartItem(menuItemId = "m1", name = "Item m1", price = 12.0, imageUrl = "", quantity = 2),
            items.first()
        )
    }

    @Test
    fun `addToCart twice for the same item overwrites rather than duplicating`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 1)
        repository.addToCart(menuItem("m1"), quantity = 3)

        val items = repository.getCartItems().first()

        assertEquals(1, items.size)
        assertEquals(3, items.first().quantity)
    }

    @Test
    fun `updateQuantity to zero removes the item`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 1)

        repository.updateQuantity("m1", 0)

        assertTrue(repository.getCartItems().first().isEmpty())
    }

    @Test
    fun `updateQuantity to a positive value updates the existing item`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 1)

        repository.updateQuantity("m1", 5)

        assertEquals(5, repository.getCartItems().first().first().quantity)
    }

    @Test
    fun `removeItem removes only the matching entry`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 1)
        repository.addToCart(menuItem("m2"), quantity = 1)

        repository.removeItem("m1")

        val items = repository.getCartItems().first()
        assertEquals(1, items.size)
        assertEquals("m2", items.first().menuItemId)
    }

    @Test
    fun `clearCart empties all items`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 1)
        repository.addToCart(menuItem("m2"), quantity = 1)

        repository.clearCart()

        assertTrue(repository.getCartItems().first().isEmpty())
    }

    @Test
    fun `getCartItemCount sums quantities across all items`() = runTest {
        repository.addToCart(menuItem("m1"), quantity = 2)
        repository.addToCart(menuItem("m2"), quantity = 3)

        assertEquals(5, repository.getCartItemCount().first())
    }
}
