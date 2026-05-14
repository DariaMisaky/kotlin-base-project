package com.daria.kotlinbase.domain.usecases

import com.daria.kotlinbase.domain.abstractions.ProductRepository
import com.daria.kotlinbase.domain.entities.Product
import com.daria.kotlinbase.shared.base.Result
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductsUseCaseTest {
    private lateinit var sut: GetProductsUseCase
    private lateinit var mockRepo: ProductRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk()
        sut = GetProductsUseCase(repo = mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `executeNow calls repository`() =
        runTest {
            coEvery { mockRepo.getProducts() } returns mockProducts

            sut.executeNow(Unit)

            coVerify(exactly = 1) { mockRepo.getProducts() }
        }

    @Test
    fun `executeNow with success returns Result Success`() =
        runTest {
            coEvery { mockRepo.getProducts() } returns mockProducts

            val result = sut.executeNow(Unit)

            assertTrue(result is Result.Success)
            assertEquals(mockProducts, (result as Result.Success).data)
        }

    @Test
    fun `executeNow with exception returns Result Error`() =
        runTest {
            coEvery { mockRepo.getProducts() } throws IOException("Network error")

            val result = sut.executeNow(Unit)

            assertTrue(result is Result.Error)
        }

    companion object {
        private val mockProducts =
            listOf(
                Product(
                    id = 1,
                    title = "Backpack",
                    price = 42.5,
                    description = "Spacious",
                    category = "men's clothing",
                    imageUrl = "https://example.com/img.jpg",
                    rating = 4.5,
                    ratingCount = 120,
                ),
            )
    }
}
