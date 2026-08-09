package com.vaibhav.restaurant.core.common

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `asResource emits Loading then Success on successful flow`() = runTest {
        val source = flow { emit("data") }

        val results = source.asResource().toList()

        assertEquals(listOf(Resource.Loading, Resource.Success("data")), results)
    }

    @Test
    fun `asResource emits Loading then Error when upstream throws`() = runTest {
        val failure = IllegalStateException("boom")
        val source = flow<String> { throw failure }

        val results = source.asResource().toList()

        assertEquals(2, results.size)
        assertEquals(Resource.Loading, results[0])
        val error = results[1] as Resource.Error
        assertEquals("boom", error.message)
        assertTrue(error.throwable === failure)
    }
}
